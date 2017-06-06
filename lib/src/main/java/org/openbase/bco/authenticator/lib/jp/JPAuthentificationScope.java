package org.openbase.bco.authenticator.lib.jp;

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

import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import rsb.Scope;

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class JPAuthentificationScope extends JPScope {

    public final static String[] COMMAND_IDENTIFIERS = {"--authentification-scope"};

    public JPAuthentificationScope() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Scope getPropertyDefaultValue() {
        return super.getPropertyDefaultValue().concat(new Scope("/bco/authentification"));
    }

    @Override
    public String getDescription() {
        return "Setup the authentification scope which is used for the rsb communication.";
    }
}
