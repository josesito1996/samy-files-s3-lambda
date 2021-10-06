package com.amazonaws.lambda.demo.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
@Setter
@ToString
public class Request {

    private String httpMethod;
    
    private String idFile;

    private String type;

    private String fileName;
    
    private List<FileRequestBody> requestBody;
}
