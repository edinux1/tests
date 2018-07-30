package com.mycorp;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAsyncCompletionHandler<T> extends ZendeskAsyncCompletionHandler<T> {
    private final Class<T> clazz;
    private final String name;
    private final Class[] typeParams;
    private final ObjectMapper mapper;
    private final Logger logger;


    public BasicAsyncCompletionHandler(Class clazz, String name, Class... typeParams) {
        this.clazz = clazz;
        this.name = name;
        this.typeParams = typeParams;
        this.logger = LoggerFactory.getLogger(clazz.getClass());
        this.mapper = createMapper();
    }

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private boolean isRateLimitResponse(Response response) {
        return response.getStatusCode() == 429;
    }

    @Override
    public T onCompleted(Response response) throws Exception {
        logResponse(response);
        if (isStatus2xx(response)) {
            if (typeParams.length > 0) {
                JavaType type = mapper.getTypeFactory().constructParametricType(clazz, typeParams);
                return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), type);
            }
            return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), clazz);
        } else if (isRateLimitResponse(response)) {
            throw new ZendeskException(response.toString());
        }

        if (response.getStatusCode() == 404) {
            return null;
        }
        throw new ZendeskException(response.toString());
    }

    private boolean isStatus2xx(Response response) {
        return response.getStatusCode() / 100 == 2;
    }

    private void logResponse(Response response) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Response HTTP/{} {}\n{}", response.getStatusCode(), response.getStatusText(),
                response.getResponseBody());
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Response headers {}", response.getHeaders());
        }
    }
}
