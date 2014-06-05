package com.test.rest.csv;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Class to store configuration of how to convert objects of a class to
 * CSV fields.
 * </p>
 * 
 * @author sergey
 */
public class CsvMapperConfig {
    private Class[] _toStringClasses;
    private Class[] _pojoClasses;
    private boolean pojoDefault;
    private Map<Class, PojoAttributeMapping> _pojoAttibutes;
    private boolean _writeHeaders;

    public CsvMapperConfig() {
        _pojoAttibutes = new HashMap<Class, PojoAttributeMapping>();
    }

    /**
     * <p>
     * Returns configured mapping for the class. 
     * </p><p>
     * If the class does not have a mapping explicitly defined then:
     * <ul>
     * <li>if default POJO but class should be represented as toString, 
     *      mapping is null</li>
     * <li>if default is not POJO, class specified in toStringClasses array
     *      will have null mapping</li>
     * <li>otherwise default POJO mapping will be provided (all public bean 
     *      properties - via getters - will be mapped to the csv fields that are 
     *      named the same), if a property is a POJO, it will be mapped according
     *      to the same rules (and it will be flatten out, so property fields will
     *      be listed on the 'top' level)</li>
     * </ul>
     * </p><p>
     * 
     * @param aClass  a class to get properties to csv mapping
     * @return  a mapping that describes how to render an class instances as CSV rows
     */
    public PojoAttributeMapping getAttributeMapping(Class aClass) {
        for( Class mappedClass : _pojoAttibutes.keySet() ) {
            if( mappedClass.isAssignableFrom(aClass) ) {
                return _pojoAttibutes.get(mappedClass);
            }
        }
        
        boolean needDefaultClass = true;
        if( !pojoDefault ) {           
            needDefaultClass = false;
            if( _pojoClasses != null ) {
                for(Class pojoClass : _pojoClasses) {
                    if( aClass == pojoClass )  {
                        needDefaultClass = true;
                        break;
                    }
                }
            }
        } else {
            if( _toStringClasses != null ) {
                for(Class strClass : _toStringClasses) {
                    if( aClass == strClass )  {
                        needDefaultClass = true;
                        break;
                    }
                }
            }
        }
        
        if( !needDefaultClass ) {
            return null;
        }

        Class componentClass = aClass.getComponentType();
        if( componentClass != null || aClass.isAssignableFrom(Collection.class) ) {
            _pojoAttibutes.put(aClass, null);
            if( componentClass != null ) {
                aClass = componentClass;
            } else {
                TypeVariable[] typeParameters = aClass.getTypeParameters();                
                if( typeParameters != null && typeParameters.length > 0 ) {
                    aClass = typeParameters[0].getClass();
                }
            }
            
            if( aClass != null ) { // prepare classes for array/collection elements in advance
                getAttributeMapping(aClass);
            }
            
            return null; // array/collection itself does not have a mapping
        }
        
        PojoAttributeMapping defaultPojoMapping = getDefaultPojoMapping(aClass);
        _pojoAttibutes.put(aClass, defaultPojoMapping);
        
        
        return defaultPojoMapping;
    }

    /**
     * Assign attribute mapping to be used for the class.
     * 
     * @param aClass  a class to use the mapping for
     * @param pojoAttributeMapping  a class properties to csv fields mapping
     */
    public void setAttributeMapping(Class aClass, PojoAttributeMapping pojoAttributeMapping) {
        this._pojoAttibutes.put(aClass, pojoAttributeMapping);
    }

    /**
     * Returns class that are always represented as string {@code toString()}
     * is called on the instances.
     * 
     * @return  an array of the classes that are always serialized as the result
     *          of toString method
     */
    public Class[] getToStringClasses() {
        return _toStringClasses;
    }

    /**
     * Returns true if the class need to be serialized using its toString method,
     * false if it has a attribute mapping (either explicit or default).
     * 
     * @param aClass  a class to check 
     * @return   true if class needs to be serialized as toString string, false otherwise
     */
    public boolean needsToString(Class aClass) {
        // Explicitly string?
        if( _toStringClasses != null ) {
            for( Class strClass : _toStringClasses ) {
                if( strClass.isAssignableFrom(aClass) ) {
                    return true;
                }
            }
        }

        // Not a pojo by default, but might be explicit pojo
        if( !pojoDefault && _pojoClasses != null ) {
            // Explicitly pojo?
            for( Class pojoClass : _pojoClasses ) {
                if( pojoClass.isAssignableFrom(aClass) ) {
                    return false;
                }
            }
        }

        // neither explicit string or pojo
        return !pojoDefault;
    }

    /**
     * Specifies which classes to be serialized using their toString method.
     * 
     * @param toStringClasses  an array of 'toString' classes
     */
    public void setToStringClasses(Class ... toStringClasses) {
        _toStringClasses = toStringClasses;
    }

    /**
     * Returns classes that are always serialized using POJO mapping, even
     * if they don't have explicit mapping and POJO serialization is not 
     * enabled by default.
     * 
     * @return   array of classes always rendered as POJOs
     */
    public Class[] getPojoClasses() {
        return _pojoClasses;
    }

    /**
     * Sets classes that are always serialized using POJO mapping, even
     * if they don't have explicit mapping and POJO serialization is not 
     * enabled by default.
     * 
     * @param pojoClasses  array of classes always rendered as POJOs
     */
    public void setPojoClasses(Class ... pojoClasses) {
        _pojoClasses = pojoClasses;
    }

    /**
     * <p>Returns true if by default all objects are treated as POJO and
     * their public properties are converted to CSV fields separately.
     * </p><p>
     * If false, the objects are converted to CSV just as a single value
     * that is toString method response from the object.
     * </p>
     * 
     * @return 
     */
    public boolean isPojoDefault() {
        return pojoDefault;
    }

    /**
     * <p>Returns true if by default all objects are treated as POJO and
     * their public properties are converted to CSV fields separately.
     * </p><p>
     * If false, the objects are converted to CSV just as a single value
     * that is toString method response from the object.
     * </p><p>
     * Set it to true with the caution. The serializer will go through the 
     * deep recursion when writing out the objects.
     * </p>
     *
     * @param pojoDefault
     */
    public void setPojoDefault(boolean pojoDefault) {
        this.pojoDefault = pojoDefault;
    }

    /**
     * Flag that tells if CSV header (first row with the column names) will be written.
     * 
     * @return   true if headers will be written, false otherwise
     */
    public boolean isWriteHeaders() {
        return _writeHeaders;
    }

    /**
     * Sets flag that tells if CSV header (first row with the column names) will be written.
     * 
     * @param writeHeaders   true if to write header, false otherwise
     */
    public void setWriteHeaders(boolean writeHeaders) {
        _writeHeaders = writeHeaders;
    }

    private PojoAttributeMapping getDefaultPojoMapping(Class aClass) {
        PojoAttributeMapping pojoMapping = new PojoAttributeMapping();
        Method[] methods = aClass.getDeclaredMethods();
        for(Method method : methods) {
            if( !Modifier.isPublic(method.getModifiers()) ) {
                continue; // ignore non public methods
            }
            String methodName = method.getName();
            int prefixLen = 0;
            if( methodName.startsWith("get") ) {
                prefixLen = 3;
            } else if( methodName.startsWith("is") ) {
                prefixLen = 2;
            }
            if( prefixLen > 0 && methodName.length() > prefixLen ) {
                String attrName = Character.toLowerCase(methodName.charAt(prefixLen)) + methodName.substring(prefixLen+1);                
                PojoCsvAttribute attribute;
                
                PojoAttributeMapping attrMapping = null;
                Class<?> attrClass = method.getReturnType();                
                if( !attrClass.isPrimitive() && attrClass != String.class ) { // assuming single class loader
                    attrMapping = getAttributeMapping(attrClass);
                    attribute = new PojoCsvAttribute(attrName, attrMapping);
                } else {
                    attribute = new PojoCsvAttribute(attrName, attrName);
                }
                
                pojoMapping.add(attribute);
            }
        }
        
        return pojoMapping;
    }
}
