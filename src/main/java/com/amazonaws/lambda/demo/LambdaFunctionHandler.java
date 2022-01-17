package com.amazonaws.lambda.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.lambda.demo.model.Archivo;
import com.amazonaws.lambda.demo.model.FileRequestBody;
import com.amazonaws.lambda.demo.model.Request;
import com.amazonaws.lambda.demo.util.Constants;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class LambdaFunctionHandler implements RequestHandler<Request, Object> {

    private AmazonS3 s3Client = s3Client();
    private AmazonDynamoDB dynamoDb = amazonDynamoDB();

    public AmazonS3 s3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(Constants.accessKey,
                Constants.secretKey);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Constants.region).build();
    }

    public AwsClientBuilder.EndpointConfiguration endpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(Constants.serviceEnpoint,
                Constants.region);
    }

    public AWSCredentialsProvider awsCredentialsProvider() {
        return new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(Constants.accessKey, Constants.secretKey));
    }

    public AmazonDynamoDB amazonDynamoDB() {

        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration())
                .withCredentials(awsCredentialsProvider()).build();
    }

    @SuppressWarnings("restriction")
    @Override
    public Object handleRequest(Request request, Context context) {
        switch (request.getHttpMethod()) {
        case "GET":
            try {
                S3Object object = s3Client.getObject(Constants.bucketName, request.getIdFile());
                S3ObjectInputStream objectInputStream = object.getObjectContent();
                return "data:".concat(request.getType()).concat(";base64,")
                        .concat(new sun.misc.BASE64Encoder()
                                .encode(IOUtils.toByteArray(objectInputStream)))
                        .replaceAll("\n", "");
            } catch (AmazonServiceException | IOException e) {
                e.printStackTrace();
            }
            break;
        case "POST":
            try {
            	String bucketName = request.getBucketName() != null ? Constants.bucketNameResource : Constants.bucketName;
                for (FileRequestBody file : request.getRequestBody()) {
                    File archivo = archivoFromBase64(file);
                    Map<String, String> metadata = new HashMap<>();
                    // metadata.put("Content-Type", file.getTipo());
                    metadata.put("Content-length", String.valueOf(archivo.length()));
                    InputStream is = new FileInputStream(archivo);
                    upload(bucketName, file.getIdFile().concat(getExtension(file.getNombreArchivo())), Optional.of(metadata), is, file);
                    archivo.delete();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        default:
            break;
        }
        return "---";
    }

    private Archivo persistData(FileRequestBody fileRequestBody, boolean estado) {
        try {
            DynamoDBMapper mapper = new DynamoDBMapper(this.dynamoDb);
            Archivo archivo = Archivo.builder().nombreArchivo(fileRequestBody.getNombreArchivo())
                    .fechaRegistro(fechaFormateada(LocalDateTime.now())).estaCargado(estado)
                    .build();
            mapper.save(archivo);
            return mapper.load(archivo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File archivoFromBase64(FileRequestBody requestBody) {
        File tempFile;
        try {
            tempFile = File.createTempFile("archivoTemp",
                    getExtension(requestBody.getNombreArchivo()));
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] decodedFile = Base64.getDecoder().decode(
                        base64Text(requestBody.getBase64()).getBytes(StandardCharsets.UTF_8));
                fos.write(decodedFile);
                return tempFile;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }
    }

    public static String base64Text(String base64) {
        return base64.isEmpty() || base64 == null ? "" : base64.split(",")[1];
    }

    private void upload(String path, String fileName,
            Optional<Map<String, String>> optionalMetaData, InputStream input,
            FileRequestBody request) {
        boolean exito = false;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        optionalMetaData.ifPresent(map -> {
            if (!map.isEmpty()) {
                map.forEach(objectMetadata::addUserMetadata);
            }
        });
        try {
            s3Client.putObject(path, fileName, input, objectMetadata);
            exito = true;
        } catch (AmazonServiceException e) {
            throw new IllegalStateException("Error al subir archivo");
        }
        persistData(request, exito);
    }

    private static String getExtension(String fileName) {
        return fileName.substring(fileName.indexOf("."), fileName.length());
    }

    public static String fechaFormateada(LocalDateTime fecha) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        return fecha.format(formatter);
    }
}