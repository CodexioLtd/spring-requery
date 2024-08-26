package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;

public interface HttpFilterAdapter {
    boolean supports(HttpServletRequest req);

    FilterRequestWrapper adapt(HttpServletRequest req) throws JsonProcessingException;
}
