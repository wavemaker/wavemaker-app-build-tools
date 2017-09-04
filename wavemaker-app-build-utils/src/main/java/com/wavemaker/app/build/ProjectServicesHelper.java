package com.wavemaker.app.build;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectServicesHelper {

    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String SERVICE_DEF_XML = "servicedef.xml";

    public static String findServiceType(Folder serviceFolder){
        final File serviceDefXML = serviceFolder.getFolder(DESIGN_TIME_FOLDER).getFile(SERVICE_DEF_XML);
        if (!serviceDefXML.exists()) {
            return null;
        }
        String serviceType;
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        InputStream is = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            is = serviceDefXML.getContent().asInputStream();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            serviceType = doc.getDocumentElement().getAttribute("type");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new WMRuntimeException("failed to find serviceType from servicedef xml file", e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(is);
        }
        return serviceType;

    }

}
