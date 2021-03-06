/**
 * Copyright (C) <2020>  <chen junwen>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
package io.mycat.replica;

public enum InstanceType {
    READ(false, true),
    WRITE(true, false),
    READ_WRITE(true, true);

    private boolean writeType;
    private boolean readType;

    InstanceType(boolean writeType, boolean readType) {
        this.writeType = writeType;
        this.readType = readType;
    }

    public boolean isWriteType() {
        return writeType;
    }

    public boolean isReadType() {
        return readType;
    }
}