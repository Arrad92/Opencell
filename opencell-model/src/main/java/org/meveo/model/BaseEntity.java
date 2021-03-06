/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/**
 * Base class for all entity classes.
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable, IEntity, IVersionedEntity {
	private static final long serialVersionUID = 1L;

	public static final int NB_PRECISION = 23;
	public static final int NB_DECIMALS = 12;
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(generator = "ID_GENERATOR", strategy = GenerationType.AUTO)
	@Column(name = "ID")
	@Access(AccessType.PROPERTY)
	protected Long id;

	@Version
	@Column(name = "VERSION")
	private Integer version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public boolean isTransient() {
		return id == null;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Equals method must be overridden in concrete Entity class. Entities
	 * shouldn't be compared only by ID, because if entity is not persisted its
	 * ID is null.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		System.out.println("this .class"+this.getClass()+" this:"+this+" obj"+obj+" obj.class"+obj.getClass());
		throw new IllegalStateException("Equals method was not overriden!");
	}


	@Override
    public String toString() {
        return String.format("id=%s", id);
    }
}