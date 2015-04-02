/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.jobs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.ScheduleExpression;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.meveo.model.Auditable;
import org.meveo.model.BaseEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.crm.CustomFieldInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "MEVEO_TIMER")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "MEVEO_TIMER_SEQ")
public class TimerEntity extends BaseEntity implements ICustomFieldEntity {

	private static final long serialVersionUID = -3764934334462355788L;

	@Transient
	private Logger log = LoggerFactory.getLogger(TimerEntity.class);

	@Column(name = "NAME", nullable = false, unique=true)
	private String name;

	@Column(name = "JOB_NAME", nullable = false)
	private String jobName;

	@Embedded
	private TimerInfo timerInfo = new TimerInfo();

	@Transient
	private TimerEntity followingTimer;

	@Column(name = "SC_YEAR", nullable = false)
	private String year = "*";

	@Column(name = "SC_MONTH", nullable = false)
	private String month = "*";

	@Column(name = "SC_D_O_MONTH", nullable = false)
	private String dayOfMonth = "*";

	@Column(name = "SC_D_O_WEEK", nullable = false)
	private String dayOfWeek = "*";

	@Column(name = "SC_HOUR", nullable = false)
	private String hour = "*";

	@Column(name = "SC_MIN", nullable = false)
	private String minute = "0";

	@Column(name = "SC_SEC", nullable = false)
	private String second = "0";

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "SC_START", nullable = true)
	private Date start;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "SC_END", nullable = true)
	private Date end;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "JOB_CATEGORY")
	JobCategoryEnum jobCategoryEnum;
	
	@OneToMany(mappedBy = "timerEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@MapKeyColumn(name = "code")
	private Map<String, CustomFieldInstance> customFields = new HashMap<String, CustomFieldInstance>();


	public String getName() {
		return (name == null) ? (getId() == null ? null : jobName + "_"
				+ getId()) : name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		System.out.println("setJobName(" + jobName + ")");
		this.jobName = jobName;
		timerInfo.setJobName(jobName);
	}

	public TimerInfo getTimerInfo() {
		return timerInfo;
	}

	public void setTimerInfo(TimerInfo timerInfo) {
		this.timerInfo = timerInfo;
	}

	public TimerEntity getFollowingTimer() {
		return followingTimer;
	}

	public void setFollowingTimer(TimerEntity followingTimer) {
		this.followingTimer = followingTimer;
		if (followingTimer != null) {
			this.timerInfo.setFollowingTimerId(followingTimer.getId());
		}
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public String getHour() {
		return hour;
	}

	public void setHour(String hour) {
		this.hour = hour;
	}

	public String getMinute() {
		return minute;
	}

	public void setMinute(String minute) {
		this.minute = minute;
	}

	public String getSecond() {
		return second;
	}

	public void setSecond(String second) {
		this.second = second;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public ScheduleExpression getScheduleExpression() {
		ScheduleExpression expression = new ScheduleExpression();
		expression.dayOfMonth(dayOfMonth);
		expression.dayOfWeek(dayOfWeek);
		expression.end(end);
		expression.hour(hour);
		expression.minute(minute);
		expression.month(month);
		expression.second(second);
		expression.start(start);
		expression.year(year);
		return expression;
	}

	public String getTimerSchedule() {
		String result = "";
		try {
			result = getScheduleExpression().toString();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof TimerEntity) {
			if (this == other) {
				return true;
			}
			TimerEntity timer = (TimerEntity) other;

			if (this.getId() == timer.getId()) {
				return true;
			}
			if (StringUtils.equals(jobName, timer.getJobName())
					&& StringUtils.equals(name, timer.getName())
					&& StringUtils.equals(getScheduleExpression().toString(),
							timer.getScheduleExpression().toString())
					&& timerInfo.equals(timer.getTimerInfo())) {
				return true;
			}
		}
		return false;
	}

	public JobCategoryEnum getJobCategoryEnum() {
		return jobCategoryEnum;
	}

	public void setJobCategoryEnum(JobCategoryEnum jobCategoryEnum) {
		this.jobCategoryEnum = jobCategoryEnum;
	}

    public Map<String, CustomFieldInstance> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, CustomFieldInstance> customFields) {
        this.customFields = customFields;
    }
    private CustomFieldInstance getOrCreateCustomFieldInstance(String code) {
        CustomFieldInstance cfi = null;

        if (customFields.containsKey(code)) {
            cfi = customFields.get(code);
        } else {
            cfi = new CustomFieldInstance();
            Auditable au = new Auditable();
            au.setCreated(new Date());
            cfi.setAuditable(au);
            cfi.setCode(code);
            cfi.setTimerEntity(this);
            cfi.setProvider(this.getProvider());
            customFields.put(code, cfi);
        }

        return cfi;
    }

    public String getStringCustomValue(String code) {
        String result = null;
        if (customFields.containsKey(code)) {
            result = customFields.get(code).getStringValue();
        }

        return result;
    }

    public void setStringCustomValue(String code, String value) {
        getOrCreateCustomFieldInstance(code).setStringValue(value);
    }

    public Date getDateCustomValue(String code) {
        Date result = null;
        if (customFields.containsKey(code)) {
            result = customFields.get(code).getDateValue();
        }

        return result;
    }

    public void setDateCustomValue(String code, Date value) {
        getOrCreateCustomFieldInstance(code).setDateValue(value);
    }

    public Long getLongCustomValue(String code) {
        Long result = null;
        if (customFields.containsKey(code)) {
            result = customFields.get(code).getLongValue();
        }
        return result;
    }

    public void setLongCustomValue(String code, Long value) {
        getOrCreateCustomFieldInstance(code).setLongValue(value);
    }

    public Double getDoubleCustomValue(String code) {
        Double result = null;

        if (customFields.containsKey(code)) {
            result = customFields.get(code).getDoubleValue();
        }

        return result;
    }

    public void setDoubleCustomValue(String code, Double value) {
        getOrCreateCustomFieldInstance(code).setDoubleValue(value);
    }

    public String getCustomFieldsAsJson() {
        String result = "";
        String sep = "";

        for (Entry<String, CustomFieldInstance> cf : customFields.entrySet()) {
            result += sep + cf.getValue().toJson();
            sep = ";";
        }

        return result;
    }
    

    public String getInheritedCustomStringValue(String code) {
        String result = null;
        if (getCustomFields().containsKey(code) && getCustomFields().get(code).getStringValue() != null) {
            result = getCustomFields().get(code).getStringValue();
        } 
        return result;
    }

    public Long getInheritedCustomLongValue(String code) {
        Long result = null;
        if (getCustomFields().containsKey(code) && getCustomFields().get(code).getLongValue() != null) {
            result = getCustomFields().get(code).getLongValue();
        } 
        return result;
    }

    public Date getInheritedCustomDateValue(String code) {
        Date result = null;
        if (getCustomFields().containsKey(code) && getCustomFields().get(code).getDateValue() != null) {
            result = getCustomFields().get(code).getDateValue();
        } 
        return result;
    }

    public Double getInheritedCustomDoubleValue(String code) {
        Double result = null;
        if (getCustomFields().containsKey(code) && getCustomFields().get(code).getDoubleValue() != null) {
            result = getCustomFields().get(code).getDoubleValue();
            
        }
        return result;
    }

    public String getICsv(String code) {
        return getInheritedCustomStringValue(code);
    }

    public Long getIClv(String code) {
        return getInheritedCustomLongValue(code);
    }

    public Date getICdav(String code) {
        return getInheritedCustomDateValue(code);
    }

    public Double getICdov(String code) {
        return getInheritedCustomDoubleValue(code);
    }

}
