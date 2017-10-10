package org.openbase.bco.authentication.lib.jp;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPTime;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPSessionTimeout extends AbstractJPTime {

    public final static String[] COMMAND_IDENTIFIERS = {"--session-timeout"};
    
    private static final long MILLI_IN_SECOND = 1000;
    private static final long SECOND_IN_MINUTE = 60;
    private static final long DEFAULT_TIMEOUT = 15 * SECOND_IN_MINUTE * MILLI_IN_SECOND;

    public JPSessionTimeout() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Long getPropertyDefaultValue() throws JPNotAvailableException {
        if (JPService.testMode()) {
            return MILLI_IN_SECOND;
        }
        return DEFAULT_TIMEOUT;
    }

    @Override
    public String getTimeDescription() {
        return "Set the session timeout.";
    }
}
