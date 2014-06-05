package com.test.rest.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to serialize objects to csv using the instructions in the configuration 
 * provided to the constructor.
 * 
* @author sergey
*/
public class PojoToCsvSerializer {
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String SPACE = " ";
    public static final String VALUE_SEPARATOR = ",";  //must not have a space after the separator
    public static final String SPACED_VALUE_SEPARATOR = "," + SPACE;
    public static final String QUOTE = "\"";
    public static final String ESCAPED_QUOTE = "\"\"";
    public static final String NULL_TEXT = "null";
    public static final String ZERO_PRESERVER = "=";

    private boolean _improveFormattingForExcel;

    private final CsvMapperConfig _config;

    public PojoToCsvSerializer(CsvMapperConfig config) {
        _config = config;
    }

    public void serialize(Object obj, OutputStream stream) throws IOException {
        if( _config.isWriteHeaders() ) {
            writeHeaders(obj, stream);
        }

        if( obj != null ) {
            final PojoAttributeMapping pojoAttributeMapping = _config.getAttributeMapping(obj.getClass());

            writeContent(obj, stream, true, pojoAttributeMapping, true);
        }
    }

    public CsvMapperConfig getConfig() {
        return _config;
    }
    
    public boolean isImproveFormattingForExcel() {
        return _improveFormattingForExcel;
    }

    public void setImproveFormattingForExcel(boolean improveFormattingForExcel) {
        _improveFormattingForExcel = improveFormattingForExcel;
    }

    protected void writeHeaders(Object obj, OutputStream stream) throws IOException {
        while( obj instanceof Iterable) {
            Iterator itr = ((Iterable)obj).iterator();
            if( !itr.hasNext() ) {
                return;
            }
            obj = itr.next();
        }
        
        while( obj != null && obj.getClass().isArray() ) {
            if( Array.getLength(obj) == 0 ) {
                return;
            }
            obj = Array.get(obj, 0);
        }
        
        if( obj == null ) {
            return;
        }
        
        Class objClass = obj.getClass();

        final PojoAttributeMapping attributeMapping = _config.getAttributeMapping(objClass);
        if( attributeMapping != null ) {
            if( _config.isWriteHeaders() ) {
                writeHeaders(stream, attributeMapping, "");
                writeRaw(stream, NEWLINE);
            }

            prepareGetters(objClass, _config.getAttributeMapping(objClass));
        }
    }

    protected void prepareGetters(Class objectClass, final PojoAttributeMapping pojoAttributeMapping) {
        // skip collections - methods are for the elements of the collection, not the collections,
        // but we don't know the class of the elements in the collection - will figure getters later, when actually
        // processing the elements
        if( objectClass.getComponentType() != null || Collection.class.isAssignableFrom(objectClass) ) {
            return;
        }
        
        for(PojoCsvAttribute pojoCsvAttribute : pojoAttributeMapping.getPojoAttributes() ) {
            String[] attributeNames = pojoCsvAttribute.getAttributeName();
            LinkedList<Object> getters = new LinkedList<Object>();
            for(String attributeName : attributeNames) {
                attributeName = attributeName.substring(0,1).toUpperCase() + (attributeName.length() == 1 ? "" : attributeName.substring(1));

                Object getter;
                try {
                    getter = objectClass.getMethod("get" + attributeName);
                } catch (NoSuchMethodException e) {
                    try {
                        getter = objectClass.getMethod("is" + attributeName);
                    } catch (NoSuchMethodException ex) {
                        //throw new IllegalArgumentException("No getter (is/get) found for " + attributeName + " on " + objectClass.getSimpleName());
                        getter = attributeName;
                    }
                }
                
                getters.add(getter);
                
                final PojoAttributeMapping childAttributeMapping = pojoCsvAttribute.getAttributeMapping();
                if( (getter instanceof Method) && childAttributeMapping != null ) {
                    prepareGetters(((Method)getter).getReturnType(), childAttributeMapping);
                }
            }

            Object[] getterAr = new Object[getters.size()];            
            pojoCsvAttribute.setGetter(getters.toArray(getterAr));
        }
    }

    protected void writeHeaders(OutputStream stream, PojoAttributeMapping attributeMapping, String div) throws IOException {
        for(PojoCsvAttribute pojoCsvAttribute : attributeMapping.getPojoAttributes() ) {
            final PojoAttributeMapping childAttributeMapping = pojoCsvAttribute.getAttributeMapping();
            final String csvHeaderName = pojoCsvAttribute.getCsvHeaderName();
            if( csvHeaderName != null ) {
                writeRaw(stream, div);
                writeQuoted(stream, csvHeaderName);
            } else {
                writeHeaders(stream, childAttributeMapping, div);
            }
            div = VALUE_SEPARATOR;
        }
    }

    protected void writeQuoted(OutputStream stream, Object text) throws IOException {
        if( text == null ) {
            stream.write(NULL_TEXT.getBytes());
        } else {
            String textStr = text.toString();
            if( _improveFormattingForExcel && textStr.indexOf(',') == -1 ) {   // if text contains comma, adding '=' in front of it makes excel ignore quotes and split by the comma! so don't add the '=' if there is ','
                // This is to preserve leading 0, as in case 0071208413
                stream.write(ZERO_PRESERVER.getBytes());
            }

            stream.write(QUOTE.getBytes());
            stream.write(textStr.replaceAll(QUOTE, ESCAPED_QUOTE).getBytes());
            stream.write(QUOTE.getBytes());
        }
    }

    protected void writeRaw(OutputStream stream, Object text) throws IOException {
        if( text != null ) {
            stream.write(text.toString().getBytes());
        }
    }

    protected void write(OutputStream stream, Object text) throws IOException {
        if( text == null ) {
            stream.write(NULL_TEXT.getBytes());
        } else {
            String textStr = text.toString();
            boolean needQuotes = _improveFormattingForExcel;
            if( textStr.indexOf('"') != -1 ) {
                needQuotes = true;
            }

            if( needQuotes ) {
                textStr = textStr.replaceAll(QUOTE, ESCAPED_QUOTE);

                if( _improveFormattingForExcel ) {
                    // This is to avoid 'shrinking' numbers, like 9780077406691 being presented as 9.78008E+12
                    stream.write(ZERO_PRESERVER.getBytes());
                }

                stream.write(QUOTE.getBytes());
                stream.write(textStr.getBytes());
                stream.write(QUOTE.getBytes());
            } else {
                stream.write(textStr.getBytes());
            }
        }
    }

    /**
     * Serializes the given object into the stream based on the 
     * {@link PojoAttributeMapping} configuration.
     * 
     * @param obj  object to be serialized into the stream
     * @param stream  the stream to be used to output the serialized object
     * @param processCollections  if elements of the collection/array should produce a new row or not
     * @param pojoAttributeMapping  attribute mapping (serialization instruction), if any, to be used to serialize the object
     * @param needQuotes  if surround serialized text with the quotes or not
     * @throws IOException 
     */
    protected void writeContent(Object obj, OutputStream stream, boolean processCollections, PojoAttributeMapping pojoAttributeMapping, boolean needQuotes) throws IOException {
        if( obj == null ) {
            //don't write any value - keep it empty but do write separators 
            //if value that is missing corresponds to multiple columns
            List<PojoCsvAttribute> pojoAttributes = pojoAttributeMapping.getPojoAttributes();
            if( pojoAttributes != null && !pojoAttributes.isEmpty() ) {
                for(int i = pojoAttributes.size() - 1; i > 0; i--) { // write one separator less than the size
                    writeRaw(stream, VALUE_SEPARATOR);                    
                }
            }
            return;
        }

        final Class<?> objClass = obj.getClass();
        
        if( objClass.isArray() ) {
            obj = new IterableArray(obj);            
        }

        if( obj instanceof Iterable) {
            if( !processCollections ) {
                if( needQuotes ) {
                    writeRaw(stream, QUOTE);
                }
                String div = "";
                for(Object item : (Iterable)obj) {
                    writeRaw(stream, div);
                    final PojoAttributeMapping mapping =
                            (pojoAttributeMapping == null ?
                                _config.getAttributeMapping(item.getClass()) : pojoAttributeMapping);
                    writeContent(item, stream, false, mapping, false);
                    div = SPACED_VALUE_SEPARATOR; // otherwise things like "9780073371856,9780077474034" will be split into 2 columns, even comma is inside the quotes
                }
                if( needQuotes ) {
                    writeRaw(stream, QUOTE);
                }
            } else {
                for(Object item : (Iterable)obj) {
                    writeContent(item, stream, false, _config.getAttributeMapping(item.getClass()), true);
                    writeRaw(stream, NEWLINE);
                }
            }
        } else if( obj instanceof Boolean || obj instanceof Number || obj instanceof String ) {            
            if( needQuotes ) {
                if( obj instanceof String ) {
                    writeQuoted(stream, obj);
                } else {
                    write(stream, obj);
                }
            } else {
                writeRaw(stream, obj);
            }
        } else {
            final boolean needToString = (pojoAttributeMapping == null || _config.needsToString(objClass));
            if( needToString ) {
                // write as toString - it's an object but according to config it should be processed as a String
                if( needQuotes ) {
                    writeQuoted(stream, obj);
                } else {
                    writeRaw(stream, obj);
                }
            } else {
                String div = "";

                for(PojoCsvAttribute pojoCsvAttribute : pojoAttributeMapping.getPojoAttributes() ) {
                    Object[] getters = pojoCsvAttribute.getGetter();

                    if( getters == null ) { // this is probably element of the collection - getter are not prepared for those yet
                        prepareGetters(obj.getClass(), pojoAttributeMapping);
                        getters = pojoCsvAttribute.getGetter();
                    }

                    try {                        
                        writeRaw(stream, div);
                        boolean needQuotesCopy = needQuotes;
                        if( getters.length > 1 ) {
                            needQuotes = false;
                            writeRaw(stream, QUOTE);
                        }
                        for(Object getter : getters) {
                            Object value = (getter instanceof Method) ? ((Method)getter).invoke(obj) : getter;
                            PojoAttributeMapping attributeMapping = pojoCsvAttribute.getAttributeMapping();
                            writeContent(value, stream, false, attributeMapping, needQuotes);
                        }
                        if( getters.length > 1 ) {
                            writeRaw(stream, QUOTE);
                        }
                        
                        needQuotes = needQuotesCopy;

                        div = VALUE_SEPARATOR;
                    } catch (IllegalAccessException e) {
                        //                            logger.error("Exception", e);
                    } catch (InvocationTargetException e) {
                        //                            logger.error("Exception", e);
                    }
                }
            }
        }
    }

    /**
     * <p>Wrapper class for an array object to expose {@link Iterator} on
     * top of the array, this way enabling iteration over the array 
     * with {@code for} construction.
     * </p><p>
     * The {@code remove} operation is not supported by the Iterator provided.
     * </p><p>
     * The object provided must be an array so that {@code obj.getClass().isArray()}
     * returns true.
     * </p>
     */
    public static class IterableArray implements Iterable {
        private final Object ar;
        private final int len;
        
        public IterableArray(Object obj) {            
            ar = obj;
            len = Array.getLength(obj);
        }
        
        @Override
        public Iterator iterator() {
            return new Iterator() {
                private int indx = 0;

                @Override
                public boolean hasNext() {
                    return indx < len;
                }

                @Override
                public Object next() {
                    return Array.get(ar, indx++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
                
            };
        }
    }
}
