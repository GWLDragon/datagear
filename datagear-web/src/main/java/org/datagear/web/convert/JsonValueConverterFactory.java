/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.convert;

import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * {@linkplain JsonValue}转换器工厂。
 * 
 * @author datagear@163.com
 *
 * @param <S>
 * @param <T>
 */
public class JsonValueConverterFactory<S extends JsonValue, R> implements ConverterFactory<S, R>
{
	private ConversionService conversionService;

	public JsonValueConverterFactory()
	{
		super();
	}

	public JsonValueConverterFactory(ConversionService conversionService)
	{
		super();
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService()
	{
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService)
	{
		this.conversionService = conversionService;
	}

	@Override
	public <T extends R> Converter<S, T> getConverter(Class<T> targetType)
	{
		return new JsonStructureConverter<>(targetType);
	}

	protected class JsonStructureConverter<T> implements Converter<S, T>
	{
		private Class<T> targetType;

		public JsonStructureConverter()
		{
			super();
		}

		public JsonStructureConverter(Class<T> targetType)
		{
			super();
			this.targetType = targetType;
		}

		public Class<T> getTargetType()
		{
			return targetType;
		}

		public void setTargetType(Class<T> targetType)
		{
			this.targetType = targetType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T convert(S source)
		{
			if (source == null)
				return null;

			if (source instanceof JsonString)
				return (T) conversionService.convert(((JsonString) source).getString(),
						TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(targetType));
			else if (source instanceof JsonNumber)
				return (T) conversionService.convert(((JsonNumber) source).toString(),
						TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(targetType));
			else if (source instanceof JsonObject)
				return (T) convertJsonObject((JsonObject) source, targetType);
			else if (source instanceof JsonArray)
				return (T) convertJSONArray((JsonArray) source, targetType);
			else
				throw new ConversionFailedException(TypeDescriptor.forObject(source),
						TypeDescriptor.valueOf(targetType), source, new UnsupportedOperationException());
		}

		protected Object convertJsonObject(JsonObject jsonObject, Class<?> targetType)
		{
			if (Map.class.isAssignableFrom(targetType))
				return jsonObject;
			else
				return conversionService.convert(jsonObject, TypeDescriptor.valueOf(Map.class),
						TypeDescriptor.valueOf(targetType));
		}

		protected Object convertJSONArray(JsonArray jsonArray, Class<?> targetType)
		{
			if (JsonArray.class.isAssignableFrom(targetType))
				return jsonArray;
			else
				return conversionService.convert(jsonArray, TypeDescriptor.valueOf(List.class),
						TypeDescriptor.valueOf(targetType));
		}
	}
}
