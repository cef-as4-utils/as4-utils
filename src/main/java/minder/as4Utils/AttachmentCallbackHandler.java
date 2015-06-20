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

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Callback Handler implementation for the case of signing/encrypting Attachments via the SwA 
 * (SOAP with Attachments) specification.
 */
public class AttachmentCallbackHandler implements CallbackHandler {
    
    private final List<Attachment> originalRequestAttachments;
    private Map<String, Attachment> attachmentMap = new HashMap<>();
    private List<Attachment> responseAttachments = new ArrayList<>();
    
    public AttachmentCallbackHandler(List<Attachment> attachments) {
        originalRequestAttachments = attachments;
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                attachmentMap.put(attachment.getId(), attachment);
            }
        }
    }
    
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof AttachmentRequestCallback) {
                AttachmentRequestCallback attachmentRequestCallback = 
                    (AttachmentRequestCallback) callbacks[i];

                List<Attachment> attachments = 
                    getAttachmentsToAdd(attachmentRequestCallback.getAttachmentId());
                if (attachments.isEmpty()) {
                    throw new RuntimeException("wrong attachment requested");
                }
                
                attachmentRequestCallback.setAttachments(attachments);
            } else if (callbacks[i] instanceof AttachmentResultCallback) {
                AttachmentResultCallback attachmentResultCallback = 
                    (AttachmentResultCallback) callbacks[i];
                responseAttachments.add(attachmentResultCallback.getAttachment());
                attachmentMap.put(attachmentResultCallback.getAttachment().getId(), 
                                  attachmentResultCallback.getAttachment());
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    public List<Attachment> getResponseAttachments() {
        return responseAttachments;
    }
    
    // Try to match the Attachment Id. Otherwise, add all Attachments.
    private List<Attachment> getAttachmentsToAdd(String id) {
        List<Attachment> attachments = new ArrayList<>();
        if (attachmentMap.containsKey(id)) {
            attachments.add(attachmentMap.get(id));
        } else {
            if (originalRequestAttachments != null) {
                attachments.addAll(originalRequestAttachments);
            }
        }
        
        return attachments;
    }

}
