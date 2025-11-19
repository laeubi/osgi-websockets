package org.osgi.impl.websockets.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URI template parser and matcher for extracting path parameters.
 * Supports URI templates as defined in the Jakarta WebSocket specification.
 * 
 * Examples:
 * - "/param/{param1}" matches "/param/value1" and extracts param1=value1
 * - "/user/{userId}/message/{msgId}" matches "/user/123/message/456"
 */
public class URITemplate {
    
    private final String template;
    private final List<String> parameterNames;
    private final Pattern matchPattern;
    private final boolean hasParameters;
    
    /**
     * Creates a URI template from the given template string.
     * 
     * @param template the URI template string (e.g., "/param/{param1}")
     */
    public URITemplate(String template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        
        this.template = template;
        this.parameterNames = new ArrayList<>();
        this.hasParameters = template.contains("{");
        
        if (hasParameters) {
            // Build regex pattern from template
            StringBuilder regexBuilder = new StringBuilder("^");
            int start = 0;
            Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = paramPattern.matcher(template);
            
            while (matcher.find()) {
                // Add literal part before the parameter
                String literal = template.substring(start, matcher.start());
                regexBuilder.append(Pattern.quote(literal));
                
                // Extract parameter name
                String paramName = matcher.group(1);
                parameterNames.add(paramName);
                
                // Add capture group for the parameter value
                // Match any characters except '/' (path separator)
                regexBuilder.append("([^/]+)");
                
                start = matcher.end();
            }
            
            // Add remaining literal part after the last parameter
            if (start < template.length()) {
                String literal = template.substring(start);
                regexBuilder.append(Pattern.quote(literal));
            }
            
            regexBuilder.append("$");
            this.matchPattern = Pattern.compile(regexBuilder.toString());
        } else {
            // No parameters, just exact match
            this.matchPattern = null;
        }
    }
    
    /**
     * Returns true if this template contains path parameters.
     */
    public boolean hasParameters() {
        return hasParameters;
    }
    
    /**
     * Returns the list of parameter names in the order they appear in the template.
     */
    public List<String> getParameterNames() {
        return new ArrayList<>(parameterNames);
    }
    
    /**
     * Returns the original template string.
     */
    public String getTemplate() {
        return template;
    }
    
    /**
     * Tests if the given path matches this URI template.
     * 
     * @param path the path to test
     * @return true if the path matches this template
     */
    public boolean matches(String path) {
        if (path == null) {
            return false;
        }
        
        if (!hasParameters) {
            // Exact match
            return template.equals(path);
        }
        
        Matcher matcher = matchPattern.matcher(path);
        return matcher.matches();
    }
    
    /**
     * Extracts path parameters from the given path.
     * 
     * @param path the path to extract parameters from
     * @return a map of parameter names to values, or null if the path doesn't match
     */
    public Map<String, String> extractParameters(String path) {
        if (path == null || !hasParameters) {
            return new HashMap<>();
        }
        
        Matcher matcher = matchPattern.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        
        Map<String, String> parameters = new HashMap<>();
        for (int i = 0; i < parameterNames.size(); i++) {
            String paramName = parameterNames.get(i);
            String paramValue = matcher.group(i + 1);
            parameters.put(paramName, paramValue);
        }
        
        return parameters;
    }
}
