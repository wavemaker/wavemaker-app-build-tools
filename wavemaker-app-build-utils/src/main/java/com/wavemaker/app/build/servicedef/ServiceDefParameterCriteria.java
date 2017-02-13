package com.wavemaker.app.build.servicedef;

import com.wavemaker.commons.servicedef.model.Parameter;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 9/2/17
 */
public interface ServiceDefParameterCriteria {

    public boolean meetCriteria(Parameter parameter);
}
