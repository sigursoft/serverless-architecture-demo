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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class Storage implements RequestHandler<SQSEvent, Void> {

    public Void handleRequest(SQSEvent request, Context context) {
        var logger = context.getLogger();
        var dynamo = AmazonDynamoDBClientBuilder.standard().build();
        var mapper = new DynamoDBMapper(dynamo);
        var jackson = new ObjectMapper();
        List<SQSEvent.SQSMessage> messageList = request.getRecords();
        messageList.forEach(message -> {
            String content = message.getBody();
            try {
                Response validation = jackson.readValue(content, Response.class);
                mapper.save(validation);
            } catch (IOException e) {
                logger.log("Failed to parse message");
            }
        });
        return null;
    }

    @DynamoDBTable(tableName = "phone-numbers")
    public static class Response {

        String number;

        Boolean valid;

        @DynamoDBAttribute(attributeName = "Valid")
        public Boolean getValid() {
            return valid;
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
        }

        @DynamoDBHashKey(attributeName = "number")
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
