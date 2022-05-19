package com.amazonaws.lambda.demo.util;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

public class TableNameResolver extends DynamoDBMapperConfig.DefaultTableNameResolver {

	private String envProfile;

	public TableNameResolver() {
	}

	public TableNameResolver(String envProfile) {
		this.envProfile = envProfile;
	}

	@Override
	public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
		String stageName = "_".concat(envProfile);
		String rawTableName = super.getTableName(clazz, config);
		String tableName = rawTableName.concat(stageName.equals("_dev") ? "" : stageName);
		System.out.println("TAble name " + tableName);
		return tableName;
	}
}
