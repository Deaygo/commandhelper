/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.persistance;

import com.laytonsmith.core.CHVersion;
import com.laytonsmith.persistance.io.ConnectionMixinFactory;
import java.net.URI;

/**
 *
 * @author Layton
 */
//@datasource("xml")
public class XMLDataSource extends StringDataSource{
    public XMLDataSource(URI uri, ConnectionMixinFactory.ConnectionMixinOptions options) throws DataSourceException{
        super(uri, options);
    }

    @Override
    protected void populateModel(String data) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String serializeModel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public DataSourceModifier[] implicitModifiers() {
        return null;
    }

    public DataSourceModifier[] invalidModifiers() {
        return null;
    }

    public String docs() {
        return "XML {xml://path/to/xml/file.xml} --";
    }

    public CHVersion since() {
        return CHVersion.V0_0_0;
    }
}
