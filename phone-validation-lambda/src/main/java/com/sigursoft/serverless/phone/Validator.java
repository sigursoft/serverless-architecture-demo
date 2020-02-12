/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.sigursoft.serverless.phone;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class Validator implements RequestHandler<Validator.Request, Validator.Response> {

    private static final String PL = "PL";

    public Validator.Response handleRequest(Request request, Context context) {
        var logger = context.getLogger();
        var mapper = new ObjectMapper();
        var queueUrl = System.getenv("QUEUE_URL");
        var sqs = AmazonSQSClientBuilder.defaultClient();
        var phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            var parsedPhoneNumber = phoneNumberUtil.parse(request.getPhone(), PL);
            boolean isValid = phoneNumberUtil.isValidNumberForRegion(parsedPhoneNumber, PL);
            Response response = new Validator.Response(request.getPhone(), isValid);
            sqs.sendMessage(new SendMessageRequest(queueUrl, mapper.writeValueAsString(response)));
            return response;
        } catch (NumberParseException e) {
            logger.log("Failed to parse number\n");
        } catch (JsonProcessingException e) {
            logger.log("Failed to write JSON response\n");
        }
        throw new RuntimeException("Failed to process request");
    }

    static class Request {
        String phone;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Request(String phone) {
            this.phone = phone;
        }

        public Request() {
        }
    }

    static class Response {

        String number;

        Boolean valid;

        public Boolean getValid() {
            return valid;
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Response(String number, Boolean valid) {
            this.number = number;
            this.valid = valid;
        }

        public Response() {
        }
    }
}
