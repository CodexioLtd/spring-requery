package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(GraphQLComplexFilterAdapter.class)
public class GraphQLDefaultComplexFilterAdapter
        implements GraphQLComplexFilterAdapter {
    private final ObjectMapper objectMapper;

    public GraphQLDefaultComplexFilterAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(HttpServletRequest req) {
        //fix later
        return true;
    }

    @Override
    public <T> FilterRequestWrapper<T> adapt(String complexFilterJson)
            throws JsonProcessingException {
        var filterGroupRequest = this.objectMapper.readValue(
                complexFilterJson,
                FilterGroupRequest.class
        );

        return new FilterRequestWrapper<>(filterGroupRequest);
    }
}
