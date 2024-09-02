package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;

public interface GraphQLComplexFilterAdapter {
    boolean supports(HttpServletRequest req);

    <T> FilterRequestWrapper<T> adapt(String complexFilterJson) throws JsonProcessingException;
}
