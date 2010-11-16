package org.pojongo.core.conversion;

import java.beans.PropertyDescriptor;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.cfg.NamingStrategy;

import com.mongodb.DBObject;

/**
 * Default implementation of <code>DocumentToObjectConverter</code>.
 * 
 * @author Caio Filipini
 * @see org.pojongo.core.conversion.DocumentToObjectConverter
 */
public class DefaultDocumentToObjectConverter implements DocumentToObjectConverter {

	// private final Mirror mirror;
	private DBObject document;
	private NamingStrategy namingStrategy;
	
	/**
	 * Default constructor.
	 */
	DefaultDocumentToObjectConverter() {
		//this.mirror = new Mirror();
	}
	
	@Override
	public DefaultDocumentToObjectConverter from(final DBObject document) {
		if (document == null) {
			throw new IllegalArgumentException("cannot convert a null document");
		}
		this.document = document;
		return this;
	}
	
	@Override
	public <T extends Object> T to(final Class<T> objectType)
			throws IllegalStateException, IllegalArgumentException {
		if (document == null) {
			throw new IllegalStateException("cannot convert a null document, please call from(DBObject) first!");
		}
		
		T instance = instanceFor(objectType);
		PropertyDescriptor[] desc = getFieldsFor(objectType);
		
		for (PropertyDescriptor property : desc) {
			String field = property.getName();
				Object fieldValue = null;
				if ("class".equals(field)) continue;
				if ("id".equals(field)) {
					fieldValue = document.get("_id");
				} else {
					if (property.getReadMethod().isAnnotationPresent(Transient.class)){
						continue;
					}
					field = namingStrategy.propertyToColumnName(field);
					if (document.containsField(field)) {
						fieldValue = document.get(namingStrategy.propertyToColumnName(field));
					}
				}
				try {
					property.getWriteMethod().invoke(instance, fieldValue);
					//BeanUtils.setProperty(instance, field, fieldValue);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		return instance;
	}

	private <T> PropertyDescriptor[] getFieldsFor(final Class<T> objectType) {
		try {
			return PropertyUtils.getPropertyDescriptors(objectType);
					
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T instanceFor(final Class<T> objectType) {
		if (objectType == null) throw new IllegalArgumentException();
		try {
			return objectType.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}
}
