package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

public class GraphQLHttpFilterAdapter implements HttpFilterAdapter {
    @Override
    public boolean supports(HttpServletRequest req) {
        //fix later
        return true;
    }

    @Override
    public <T> FilterRequestWrapper<T> adapt(HttpServletRequest request) {
        if (request.getMethod().equals("GET")) {
            return new FilterRequestWrapper<>();
        } else if (request.getMethod().equals("POST")) {
            return new FilterRequestWrapper<>();
        } else {
            return new FilterRequestWrapper<>();
        }
    }
}
