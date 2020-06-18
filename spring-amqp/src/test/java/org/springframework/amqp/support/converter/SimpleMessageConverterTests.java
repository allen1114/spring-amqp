/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.support.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class SimpleMessageConverterTests extends AllowedListDeserializingMessageConverterTests {

	@Test
	public void bytesAsDefaultMessageBodyType() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = new Message("test".getBytes(), new MessageProperties());
		Object result = converter.fromMessage(message);
		assertThat(result.getClass()).isEqualTo(byte[].class);
		assertThat(new String((byte[]) result, "UTF-8")).isEqualTo("test");
	}

	@Test
	public void noMessageIdByDefault() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = converter.toMessage("foo", null);
		assertThat(message.getMessageProperties().getMessageId()).isNull();
	}

	@Test
	public void optionalMessageId() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		converter.setCreateMessageIds(true);
		Message message = converter.toMessage("foo", null);
		assertThat(message.getMessageProperties().getMessageId()).isNotNull();
	}

	@Test
	public void messageToString() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = new Message("test".getBytes(), new MessageProperties());
		message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
		Object result = converter.fromMessage(message);
		assertThat(result.getClass()).isEqualTo(String.class);
		assertThat(result).isEqualTo("test");
	}

	@Test
	public void messageToBytes() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = new Message(new byte[] { 1, 2, 3 }, new MessageProperties());
		message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_BYTES);
		Object result = converter.fromMessage(message);
		assertThat(result.getClass()).isEqualTo(byte[].class);
		byte[] resultBytes = (byte[]) result;
		assertThat(resultBytes.length).isEqualTo(3);
		assertThat(resultBytes[0]).isEqualTo((byte) 1);
		assertThat(resultBytes[1]).isEqualTo((byte) 2);
		assertThat(resultBytes[2]).isEqualTo((byte) 3);
	}

	@Test
	public void messageToSerializedObject() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT);
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		TestBean testBean = new TestBean("foo");
		objectStream.writeObject(testBean);
		objectStream.flush();
		objectStream.close();
		byte[] bytes = byteStream.toByteArray();
		Message message = new Message(bytes, properties);
		Object result = converter.fromMessage(message);
		assertThat(result.getClass()).isEqualTo(TestBean.class);
		assertThat(result).isEqualTo(testBean);
	}

	@Test
	public void stringToMessage() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = converter.toMessage("test", new MessageProperties());
		String contentType = message.getMessageProperties().getContentType();
		String content = new String(message.getBody(),
				message.getMessageProperties().getContentEncoding());
		assertThat(contentType).isEqualTo("text/plain");
		assertThat(content).isEqualTo("test");
	}

	@Test
	public void bytesToMessage() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message message = converter.toMessage(new byte[] { 1, 2, 3 }, new MessageProperties());
		String contentType = message.getMessageProperties().getContentType();
		byte[] body = message.getBody();
		assertThat(contentType).isEqualTo("application/octet-stream");
		assertThat(body.length).isEqualTo(3);
		assertThat(body[0]).isEqualTo((byte) 1);
		assertThat(body[1]).isEqualTo((byte) 2);
		assertThat(body[2]).isEqualTo((byte) 3);
	}

	@Test
	public void serializedObjectToMessage() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		TestBean testBean = new TestBean("foo");
		Message message = converter.toMessage(testBean, new MessageProperties());
		String contentType = message.getMessageProperties().getContentType();
		byte[] body = message.getBody();
		assertThat(contentType).isEqualTo("application/x-java-serialized-object");
		ByteArrayInputStream bais = new ByteArrayInputStream(body);
		Object deserializedObject = new ObjectInputStream(bais).readObject();
		assertThat(deserializedObject).isEqualTo(testBean);
	}

	@Test
	public void messageConversionExceptionForClassNotFound() throws Exception {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		TestBean testBean = new TestBean("foo");
		Message message = converter.toMessage(testBean, new MessageProperties());
		String contentType = message.getMessageProperties().getContentType();
		assertThat(contentType).isEqualTo("application/x-java-serialized-object");
		byte[] body = message.getBody();
		body[10] = 'z';
		assertThatThrownBy(() -> converter.fromMessage(message))
				.isExactlyInstanceOf(MessageConversionException.class)
				.hasCauseExactlyInstanceOf(IllegalStateException.class);
	}

	@Test
	public void notConvertible() {
		class Foo {

		}
		try {
			new SimpleMessageConverter().toMessage(new Foo(), new MessageProperties());
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("SimpleMessageConverter only supports String, byte[] and Serializable payloads, received:");
		}
	}

}
