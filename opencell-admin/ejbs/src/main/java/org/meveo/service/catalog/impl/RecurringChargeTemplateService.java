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
package org.meveo.service.catalog.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.meveo.model.catalog.RecurringChargeTemplate;

/**
 * Charge Template service implementation.
 * 
 */
@Stateless
public class RecurringChargeTemplateService extends ChargeTemplateService<RecurringChargeTemplate> {

	public void removeByCode(EntityManager em, String code) {
		Query query = em.createQuery("DELETE RecurringChargeTemplate t WHERE t.code=:code ");
		query.setParameter("code", code);
		query.executeUpdate();
	}

	public int getNbrRecurringChrgWithNotPricePlan() {
		return ((Long) getEntityManager()
				.createNamedQuery("recurringChargeTemplate.getNbrRecurringChrgWithNotPricePlan", Long.class)
				.getSingleResult()).intValue();
	}

	public List<RecurringChargeTemplate> getRecurringChrgWithNotPricePlan() {
		return (List<RecurringChargeTemplate>) getEntityManager()
				.createNamedQuery("recurringChargeTemplate.getRecurringChrgWithNotPricePlan",
						RecurringChargeTemplate.class)
				.getResultList();
	}

	public int getNbrRecurringChrgNotAssociated() {
		return ((Long) getEntityManager()
				.createNamedQuery("recurringChargeTemplate.getNbrRecurringChrgNotAssociated", Long.class)
				.getSingleResult()).intValue();
	}

	public List<RecurringChargeTemplate> getRecurringChrgNotAssociated() {
		return (List<RecurringChargeTemplate>) getEntityManager()
				.createNamedQuery("recurringChargeTemplate.getRecurringChrgNotAssociated",
						RecurringChargeTemplate.class)
				.getResultList();
	}

}
