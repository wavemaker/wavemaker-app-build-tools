/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.studio.app.build.exception;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 21/6/16
 */
public class ServiceDefGenerationException extends Exception {

    public ServiceDefGenerationException(String s) {
        super(s);
    }

    public ServiceDefGenerationException(String s, Throwable cause) {
        super(s, cause);
    }

    public ServiceDefGenerationException(Throwable cause) {
        super(cause);
    }
}
