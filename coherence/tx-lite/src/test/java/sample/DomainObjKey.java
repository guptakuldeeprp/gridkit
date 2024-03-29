/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DomainObjKey implements Serializable, Comparable<DomainObjKey> {

    private long id;

    public DomainObjKey() {
    }
    
    public DomainObjKey(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DomainObjKey other = (DomainObjKey) obj;
        if (id != other.id)
            return false;
        return true;
    }
    
    @Override
    public int compareTo(DomainObjKey o) {
        return (int)(id - o.id);
    }

    @Override
    public String toString() {
        return "DOK#" + id;
    }
}
