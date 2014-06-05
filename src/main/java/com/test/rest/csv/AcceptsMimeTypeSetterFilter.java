package com.test.rest.csv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * <p>
 * Executes on all the requests prior Jersey matching the resources.
 * </p></p>
 * Converts format query or extension of known types to appropriate
 * Accept HTTP header, which will be used by Jersey to choose the 
 * output mime type (thus format and MessageBodyWriter implementation).
 * </p>
 * 
 * @author sergey
 */
@PreMatching
@Provider
public class AcceptsMimeTypeSetterFilter implements ContainerRequestFilter {

    private final String _formatParameterName = "format";
    private Map<String, String> _formatToMimeMap;

    public AcceptsMimeTypeSetterFilter() {
        _formatToMimeMap = new HashMap<String, String>();
        _formatToMimeMap.put("csv", CsvObjectMapperProvider.TEXT_CSV);
        _formatToMimeMap.put("xls", CsvObjectMapperProvider.APPLICATION_EXCEL);
        _formatToMimeMap.put("json", MediaType.APPLICATION_JSON);
        _formatToMimeMap.put("xml", MediaType.APPLICATION_XML);
    }

    public Map<String, String> getFormatToMimeMap() {
        return _formatToMimeMap;
    }

    public void setFormatToMimeMap(Map<String, String> formatToMimeMap) {
        _formatToMimeMap = formatToMimeMap;
    }

    private String getExtension(String path) {
        String result = null;

        int extIndx = path.lastIndexOf('.');
        if( extIndx > 0 ) {
            if( extIndx == path.length() - 2 ) {
                return "";
            }

            result = path.substring(extIndx+1);
            if( result.indexOf('/') != -1 || result.indexOf('\\') != -1 ) {
                return null;
            }
        }

        return result;
    }

    @Override
    public void filter(ContainerRequestContext crc) throws IOException {
        UriInfo uriInfo = crc.getUriInfo();
        String formatRequested = uriInfo.getQueryParameters().getFirst(_formatParameterName);

        boolean fromExtension = false;
        if( formatRequested == null ) {
            formatRequested = getExtension(uriInfo.getPath());
            fromExtension = true;
        }

        if( formatRequested != null ) {
            String mimeType = _formatToMimeMap.get(formatRequested);
            if( mimeType != null ) {
                crc.getHeaders().putSingle(HttpHeaders.ACCEPT, mimeType); // override the Accept header based on the extension/format requested
                if( fromExtension ) { // remove 'extension' - keep the rest untouched
                    final UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
                    final String path = uriInfo.getRequestUri().getPath();
                    requestUriBuilder.replacePath(path.substring(0, path.length()-formatRequested.length()-1)); // remove the extension AND the '.'
                    crc.setRequestUri(requestUriBuilder.build());                    
                }
            }
        }
        
    }
}
