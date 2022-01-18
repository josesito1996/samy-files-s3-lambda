package com.amazonaws.lambda.demo;

import com.amazonaws.lambda.demo.model.Request;

public class app {

	public static void main(String...args) {
		LambdaFunctionHandler lambda = new LambdaFunctionHandler();
		System.out.println(lambda.handleRequest(Request.builder()
				.httpMethod("GET")
				.idFile("39ca2900-6470-47a0-9beb-bc1b4027f175.pdf")
				.type("application/pdf")
				.fileName("workPlace.pdf")
				.bucketName("")
				.build(), null));
	}
	
}
