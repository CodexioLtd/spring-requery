package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(HttpFilterAdapter.class)
public class JsonHttpFilterAdapter implements HttpFilterAdapter {
    private final ObjectMapper objectMapper;

    public JsonHttpFilterAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(HttpServletRequest req) {
        //fix later
        return true;
    }

    @Override
    public FilterRequestWrapper adapt(HttpServletRequest webRequest) throws JsonProcessingException {
        var filterJson = webRequest.getParameter("filter");
        var complexFilterJson = webRequest.getParameter("complexFilter");

        if (filterJson != null) {
            return constructSimpleFilterWrapper(filterJson);
        } else if (complexFilterJson != null) {
            return constructComplexFilterWrapper(complexFilterJson);
        } else {
            return new FilterRequestWrapper();
        }
    }

    private FilterRequestWrapper constructSimpleFilterWrapper(String filterJson) throws JsonProcessingException {
        if (!filterJson.startsWith("[")) {
            var filterRequest = this.objectMapper.readValue(
                    filterJson,
                    FilterRequest.class
            );
            return new FilterRequestWrapper(List.of(filterRequest));
        }

        return new FilterRequestWrapper(List.of(objectMapper.readValue(filterJson, FilterRequest[].class)));

    }

    private FilterRequestWrapper constructComplexFilterWrapper(String complexFilterJson) throws JsonProcessingException {
        FilterGroupRequest filterGroupRequest = this.objectMapper.readValue(
                complexFilterJson,
                FilterGroupRequest.class
        );
        return new FilterRequestWrapper(filterGroupRequest);
    }
}
