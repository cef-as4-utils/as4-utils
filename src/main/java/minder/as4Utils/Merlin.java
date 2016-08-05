/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package minder.as4Utils;

import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRL;
import java.util.Collections;
import java.util.Properties;

/**
 * A Crypto implementation based on two Java KeyStore objects, one being the keystore, and one
 * being the truststore.
 */
public class Merlin extends org.apache.wss4j.common.crypto.Merlin {


  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(Merlin.class);
  private static final boolean DO_DEBUG = LOG.isDebugEnabled();


  public Merlin() {
    // default constructor
  }

  public Merlin(boolean loadCACerts, String cacertsPasswd) {
    super(loadCACerts, cacertsPasswd);
  }

  public Merlin(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
      throws WSSecurityException, IOException {
    super(properties, loader, passwordEncryptor);
  }

  @Override
  public void loadProperties(
      Properties properties,
      ClassLoader loader,
      PasswordEncryptor passwordEncryptor
  ) throws WSSecurityException, IOException {
    if (properties == null) {
      return;
    }
    this.properties = properties;
    this.passwordEncryptor = passwordEncryptor;

    String prefix = PREFIX;
    for (Object key : properties.keySet()) {
      if (key instanceof String) {
        String propKey = (String) key;
        if (propKey.startsWith(PREFIX)) {
          break;
        } else if (propKey.startsWith(OLD_PREFIX)) {
          prefix = OLD_PREFIX;
          break;
        }
      }
    }

    //
    // Load the provider(s)
    //
    String provider = properties.getProperty(prefix + CRYPTO_KEYSTORE_PROVIDER);
    if (provider != null) {
      provider = provider.trim();
    }
    String certProvider = properties.getProperty(prefix + CRYPTO_CERT_PROVIDER);
    if (certProvider != null) {
      setCryptoProvider(certProvider);
    }
    //
    // Load the KeyStore
    //
    String alias = properties.getProperty(prefix + KEYSTORE_ALIAS);
    if (alias != null) {
      alias = alias.trim();
      setDefaultX509Identifier(alias);
    }
    String keyStoreLocation = properties.getProperty(prefix + KEYSTORE_FILE);
    if (keyStoreLocation == null) {
      keyStoreLocation = properties.getProperty(prefix + OLD_KEYSTORE_FILE);
    }
    if (keyStoreLocation != null) {
      keyStoreLocation = keyStoreLocation.trim();

      try (InputStream is = loadInputStream2(loader, keyStoreLocation)) {
        String passwd = properties.getProperty(prefix + KEYSTORE_PASSWORD, "security");
        if (passwd != null) {
          passwd = passwd.trim();
          passwd = decryptPassword(passwd, passwordEncryptor);
        }
        String type = properties.getProperty(prefix + KEYSTORE_TYPE, KeyStore.getDefaultType());
        if (type != null) {
          type = type.trim();
        }
        keystore = load(is, passwd, provider, type);
        if (DO_DEBUG) {
          LOG.debug(
              "The KeyStore " + keyStoreLocation + " of type " + type
                  + " has been loaded"
          );
        }
        String privatePasswd = properties.getProperty(prefix + KEYSTORE_PRIVATE_PASSWORD);
        if (privatePasswd != null) {
          privatePasswordSet = true;
        }
      }
    } else {
      if (DO_DEBUG) {
        LOG.debug("The KeyStore is not loaded as KEYSTORE_FILE is null");
      }
    }

    //
    // Load the TrustStore
    //
    String trustStoreLocation = properties.getProperty(prefix + TRUSTSTORE_FILE);
    if (trustStoreLocation != null) {
      trustStoreLocation = trustStoreLocation.trim();

      try (InputStream is = loadInputStream2(loader, trustStoreLocation)) {
        String passwd = properties.getProperty(prefix + TRUSTSTORE_PASSWORD, "changeit");
        if (passwd != null) {
          passwd = passwd.trim();
          passwd = decryptPassword(passwd, passwordEncryptor);
        }
        String type = properties.getProperty(prefix + TRUSTSTORE_TYPE, KeyStore.getDefaultType());
        if (type != null) {
          type = type.trim();
        }
        truststore = load(is, passwd, provider, type);
        if (DO_DEBUG) {
          LOG.debug(
              "The TrustStore " + trustStoreLocation + " of type " + type
                  + " has been loaded"
          );
        }
        loadCACerts = false;
      }
    } else {
      String loadCacerts = properties.getProperty(prefix + LOAD_CA_CERTS, "false");
      if (loadCacerts != null) {
        loadCacerts = loadCacerts.trim();
      }
      if (Boolean.valueOf(loadCacerts)) {
        String cacertsPath = System.getProperty("java.home") + "/lib/security/cacerts";
        if (cacertsPath != null) {
          cacertsPath = cacertsPath.trim();
        }
        try (InputStream is = new FileInputStream(cacertsPath)) {
          String cacertsPasswd = properties.getProperty(prefix + TRUSTSTORE_PASSWORD, "changeit");
          if (cacertsPasswd != null) {
            cacertsPasswd = cacertsPasswd.trim();
            cacertsPasswd = decryptPassword(cacertsPasswd, passwordEncryptor);
          }
          truststore = load(is, cacertsPasswd, null, KeyStore.getDefaultType());
          if (DO_DEBUG) {
            LOG.debug("CA certs have been loaded");
          }
          loadCACerts = true;
        }
      }
    }
    //
    // Load the CRL file
    //
    String crlLocation = properties.getProperty(prefix + X509_CRL_FILE);
    if (crlLocation != null) {
      crlLocation = crlLocation.trim();

      try (InputStream is = loadInputStream2(loader, crlLocation)) {
        CertificateFactory cf = getCertificateFactory();
        X509CRL crl = (X509CRL) cf.generateCRL(is);

        if (provider == null || provider.length() == 0) {
          crlCertStore =
              CertStore.getInstance(
                  "Collection",
                  new CollectionCertStoreParameters(Collections.singletonList(crl))
              );
        } else {
          crlCertStore =
              CertStore.getInstance(
                  "Collection",
                  new CollectionCertStoreParameters(Collections.singletonList(crl)),
                  provider
              );
        }
        if (DO_DEBUG) {
          LOG.debug(
              "The CRL " + crlLocation + " has been loaded"
          );
        }
      } catch (Exception e) {
        if (DO_DEBUG) {
          LOG.debug(e.getMessage(), e);
        }
        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "failedCredentialLoad");
      }
    }
  }


  /**
   * Load a KeyStore object as an InputStream, using the ClassLoader and location arguments
   */
  public static InputStream loadInputStream2(ClassLoader loader, String location)
      throws WSSecurityException, IOException {
    return SWA12Util.getResource(location);
  }
}
