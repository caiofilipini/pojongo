package org.pojongo.core.conversion;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.pojongo.test.util.MongoDBTest;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class DefaultDocumentToObjectConverterTest extends MongoDBTest {
	
	private DocumentToObjectConverter converter;

	@Before
	public void setUp() throws Exception {
		converter = new DefaultDocumentToObjectConverter();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionIfDocumentIsNull() {
		converter.from(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionIfObjectTypeIsNull() {
		converter.from(new BasicDBObject()).to(null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void shouldThrowIllegalStateExceptionIfToMethodIsCalledWithouthCallingFromMethodFirst() {
		converter.to(SimplePOJO.class);
	}
	
	@Test
	public void shouldConvertASimpleDocumentWithStringFieldsToAJavaObject() {
		DBObject document = new BasicDBObject();
		document.put("aField", "aFieldValue");
		document.put("anotherField", "anotherFieldValue");
		saveToMongo(document);
		
		DBObject docFromMongo = getFromMongo(document.get("_id"));
		
		SimplePOJO convertedObject = converter.from(docFromMongo).to(SimplePOJO.class);
		assertThat(convertedObject.getAField(), is(equalTo("aFieldValue")));
		assertThat(convertedObject.getAnotherField(), is(equalTo("anotherFieldValue")));
	}
	
	@Test
	public void shouldOnlyConvertFieldIfTheDocumentContainsAMatchingField() {
		DBObject document = new BasicDBObject();
		document.put("aField", "aFieldValue");
		saveToMongo(document);
		
		DBObject doc = spy(document);
		
		DBObject docFromMongo = getFromMongo(document.get("_id"));
		
		SimplePOJO convertedObject = converter.from(docFromMongo).to(SimplePOJO.class);
		assertThat(convertedObject.getAField(), is(equalTo("aFieldValue")));
		assertThat(convertedObject.getAnotherField(), is(nullValue()));
		
		verify(doc, never()).get("anotherField");
	}
	
	@Test
	public void shouldConvertNumericValues() {
		DBObject document = new BasicDBObject();
		document.put("anIntegerField", 42);
		document.put("aLongField", 43L);
		document.put("aDoubleField", 44.0);
		document.put("aFloatField", 45.0f);
		saveToMongo(document);
		
		DBObject docFromMongo = getFromMongo(document.get("_id"));
		
		SimplePOJO convertedObject = converter.from(docFromMongo).to(SimplePOJO.class);
		assertThat(convertedObject.getAnIntegerField(), is(equalTo(42)));
		assertThat(convertedObject.getALongField(), is(equalTo(43L)));
		assertThat(convertedObject.getADoubleField(), is(equalTo(44.0)));
	}

	@Test
	public void shouldPopulateIdWithMongosGeneratedIdValue() {
		DBObject document = new BasicDBObject();
		saveToMongo(document);
		
		Object documentId = document.get("_id");
		DBObject docFromMongo = getFromMongo(documentId);
		
		SimplePOJO convertedObject = converter.from(docFromMongo).to(SimplePOJO.class);
		assertThat(convertedObject.getId(), is(equalTo(documentId)));
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void shouldPopulateStringId() {
		DBObject document = new BasicDBObject();
		document.put("_id", "abcd1234");
		saveToMongo(document);
		
		Object documentId = document.get("_id");
		DBObject docFromMongo = getFromMongo(documentId);
		
		SimplePOJOWithStringId convertedObject = converter.from(docFromMongo).to(SimplePOJOWithStringId.class);
		Class idClass = convertedObject.getId().getClass();
		assertThat(idClass, is(equalTo(String.class)));
		assertThat(convertedObject.getId(), is(equalTo(documentId)));
	}

	@Test
	public void shouldNotPopulateTransientFields() {
		DBObject document = new BasicDBObject();
		document.put("aTransientField", "transient");
		saveToMongo(document);
		
		DBObject docFromMongo = getFromMongo(document.get("_id"));
		
		SimplePOJO simplePOJO = converter.from(docFromMongo).to(SimplePOJO.class);
		assertThat(simplePOJO.getATransientField(), is(nullValue()));
	}
	
}
