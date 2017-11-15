package com.wavemaker.app.build;

import org.w3c.dom.Document;

import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.util.XMLUtils;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectServicesHelper {

    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String SERVICE_DEF_XML = "servicedef.xml";

    public static String findServiceType(Folder serviceFolder) {
        final File serviceDefXML = serviceFolder.getFolder(DESIGN_TIME_FOLDER).getFile(SERVICE_DEF_XML);
        if (!serviceDefXML.exists()) {
            return null;
        }
        String serviceType;
        Document document = XMLUtils.getDocument(serviceDefXML);
        document.getDocumentElement().normalize();
        serviceType = document.getDocumentElement().getAttribute("type");
        return serviceType;

    }

}
