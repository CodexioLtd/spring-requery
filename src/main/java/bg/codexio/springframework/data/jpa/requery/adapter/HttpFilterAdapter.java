package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

public interface HttpFilterAdapter {
    boolean supports(HttpServletRequest req);

    <T> FilterRequestWrapper<T> adapt(HttpServletRequest req);
}
