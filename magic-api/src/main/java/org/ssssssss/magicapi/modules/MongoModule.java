package org.ssssssss.magicapi.modules;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.ssssssss.magicapi.config.MagicModule;
import org.ssssssss.magicapi.model.Constants;
import org.ssssssss.script.reflection.JavaInvoker;
import org.ssssssss.script.reflection.JavaReflection;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * mongo模块
 *
 * @author mxd
 */
public class MongoModule extends HashMap<String, MongoModule.MongoDataBaseGetter> implements MagicModule {

	private static final Logger logger = LoggerFactory.getLogger(MongoModule.class);

	private JavaInvoker<Method> invoker;

	private Object factory;

	public MongoModule(MongoTemplate mongoTemplate) {
		JavaInvoker<Method> mongoDbFactoryInvoker = JavaReflection.getMethod(mongoTemplate, "getMongoDbFactory");
		if(mongoDbFactoryInvoker == null){
			mongoDbFactoryInvoker = JavaReflection.getMethod(mongoTemplate, "getMongoDatabaseFactory");
		}
		if (mongoDbFactoryInvoker != null) {
			try {
				factory = mongoDbFactoryInvoker.invoke0(mongoTemplate, null, Constants.EMPTY_OBJECT_ARRAY);
				invoker = JavaReflection.getMethod(factory, "getDb", StringUtils.EMPTY);
				if (invoker == null) {
					invoker = JavaReflection.getMethod(factory, "getMongoDatabase", StringUtils.EMPTY);
				}
			} catch (Throwable e) {
				logger.error("mongo模块初始化失败", e);
			}
		} else {
			logger.error("mongo模块初始化失败");
		}
	}

	@Override
	public MongoDataBaseGetter get(Object databaseName) {
		try {
			if (databaseName == null) {
				return null;
			}
			MongoDatabase database = (MongoDatabase) invoker.invoke0(factory, null, new Object[]{databaseName.toString()});
			return new MongoDataBaseGetter(database);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getModuleName() {
		return "mongo";
	}

	public static class MongoDataBaseGetter extends HashMap<String, MongoCollection<Document>> {

		MongoDatabase database;

		public MongoDataBaseGetter(MongoDatabase database) {
			this.database = database;
		}

		@Override
		public MongoCollection<Document> get(Object key) {
			return database.getCollection(key.toString());
		}
	}
}
