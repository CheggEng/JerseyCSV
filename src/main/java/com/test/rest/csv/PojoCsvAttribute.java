package com.test.rest.csv;

import java.util.Arrays;

/**
 * <p>
 * A bean properties to CSV mapping.
 * </p><p>
 * It either:
 * <ul>
 * <li>represents a single complex property, that is an object itself
 * and has a mapping ({@link PojoAttributeMapping}) describing how that
 * object's properties are mapped to CSV.</li>
 * <li>or represents a single property or multiple properties of the bean that 
 * are serialized using default rules of the {@link CsvMapperConfig} and do not
 * have their own attribute mapping.</li>
 * </ul>
 * </p>
 * 
 * @author sergey
 */
public class PojoCsvAttribute {
    final private String _csvHeaderName;
    final private String _attributeName[];    
    final private PojoAttributeMapping _attributeMapping;
    private Object _getter[];

    public PojoCsvAttribute(String attributeName, PojoAttributeMapping attributeMapping) {
        _attributeName = new String[]{attributeName};
        _attributeMapping = attributeMapping;
        _csvHeaderName = null;
    }

    public PojoCsvAttribute(String csvHeaderName, String... attributeName) {
        _csvHeaderName = csvHeaderName;
        _attributeName = attributeName;
        _attributeMapping = null;
    }

    /**
     * Either getter methods to get the values representing the attribute
     * or fixed Objects to be used as a values (their toString, actually).
     * 
     * @return 
     */
    public Object[] getGetter() {
        return _getter;
    }

    /**
     * Sets either getter methods to get the values representing the attribute
     * or fixed Objects to be used as a values (normally numbers or Strings are
     * expected).
     * 
     * @param getter   array of getter/values for the attribute or CSV field
     */
    public void setGetter(Object[] getter) {
        _getter = getter;
    }

    /**
     * Returns CSV column header to be used if headers are enabled in the 
     * configuration. Can be null if attribute is an object that is represented
     * in CSV as column(s) based on its properties.
     * 
     * @return   a CSV column header name
     */
    public String getCsvHeaderName() {
        return _csvHeaderName;
    }

    /**
     * Names of a bean properties this object represents. If a property is an
     * object to be represented in CSV by its properties, the result will be
     * just a single attribute name. Otherwise it may be multiple property names
     * that are combined to represent a single CSV column.
     * 
     * @return   bean propert(y/ies) the attribute represents
     */
    public String[] getAttributeName() {
        return _attributeName;
    }

    /**
     * Returns {@link PojoAttributeMapping} when attribute is an object to 
     * be represented in CSV as its attributes as multiple columns.
     * 
     * @return   attribute mappings for the object property
     */
    public PojoAttributeMapping getAttributeMapping() {
        return _attributeMapping;
    }

    @Override
    public String toString() {
        return "{" +
                Arrays.toString(_attributeName) + ":" +
                    (_csvHeaderName == null ? "" : _csvHeaderName) +
                    (_attributeMapping == null ? "" : _attributeMapping) +
        '}';
    }
}
