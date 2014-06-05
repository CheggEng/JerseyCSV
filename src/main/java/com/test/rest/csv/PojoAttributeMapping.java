package com.test.rest.csv;

import java.util.LinkedList;
import java.util.List;

/**
* @author sergey
*/
public class PojoAttributeMapping {
    private List<PojoCsvAttribute> _pojoAttributes;

    public PojoAttributeMapping() {
        _pojoAttributes = new LinkedList<PojoCsvAttribute>();
    }

    public void add(PojoCsvAttribute pojoCsvAttribute) {
        _pojoAttributes.add(pojoCsvAttribute);
    }

    public void add(String csvHeaderName, String... attributeName) {
        _pojoAttributes.add(new PojoCsvAttribute(csvHeaderName, attributeName));
    }

    public List<PojoCsvAttribute> getPojoAttributes() {
        return _pojoAttributes;
    }

    public void setPojoAttributes(List<PojoCsvAttribute> pojoAttributes) {
        _pojoAttributes = pojoAttributes;
    }

    @Override
    public String toString() {
        return "{" +
                _pojoAttributes +
                '}';
    }
}
