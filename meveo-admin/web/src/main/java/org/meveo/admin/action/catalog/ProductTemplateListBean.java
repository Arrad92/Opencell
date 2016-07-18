package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.communication.impl.MeveoInstanceService;

@Named
@ConversationScoped
public class ProductTemplateListBean extends ProductTemplateBean {

	private static final long serialVersionUID = -7109673492144846741L;

	@Inject
	private MeveoInstanceService meveoInstanceService;

	private MeveoInstance meveoInstanceToExport = new MeveoInstance();

	private List<MeveoInstance> meveoInstances = new ArrayList<MeveoInstance>();

	private List<ProductTemplate> productTemplates = new ArrayList<ProductTemplate>();

	private List<String> bundledProducts = new ArrayList<String>();

	private List<ProductTemplate> ptToExport = new ArrayList<ProductTemplate>();

	private long activeProductCount = 0;

	private long inactiveProductCount = 0;

	private long almostExpiredCount = 0;

	@Override
	public void preRenderView() {
		productTemplates = productTemplateService.list();
		meveoInstances = meveoInstanceService.list();
		activeProductCount = productTemplateService.productTemplateCount();//productTemplateService.productTemplateActiveCount(false);
		inactiveProductCount = productTemplateService.productTemplateActiveCount(true);
		almostExpiredCount = productTemplateService.productTemplateAlmostExpiredCount();
		super.preRenderView();
	}

	@Override
	protected IPersistenceService<ProductTemplate> getPersistenceService() {
		return productTemplateService;
	}

	public String newProductTemplate() {
		return "mmProductTemplateDetail";
	}
	
	public void addProductTemplateToExport(ProductTemplate pt){
		if(!ptToExport.contains(pt)){
			ptToExport.add(pt);
		}
	}

	public List<MeveoInstance> getMeveoInstances() {
		return meveoInstances;
	}

	public void setMeveoInstances(List<MeveoInstance> meveoInstances) {
		this.meveoInstances = meveoInstances;
	}

	public MeveoInstance getMeveoInstanceToExport() {
		return meveoInstanceToExport;
	}

	public void setMeveoInstanceToExport(MeveoInstance meveoInstanceToExport) {
		this.meveoInstanceToExport = meveoInstanceToExport;
	}

	public List<ProductTemplate> getProductTemplates() {
		return productTemplates;
	}

	public void setProductTemplates(List<ProductTemplate> productTemplates) {
		this.productTemplates = productTemplates;
	}

	public List<String> getBundledProducts() {
		return bundledProducts;
	}

	public void setBundledProducts(List<String> bundledProducts) {
		this.bundledProducts = bundledProducts;
	}

	public long getActiveProductCount() {
		return activeProductCount;
	}

	public void setActiveProductCount(long activeProductCount) {
		this.activeProductCount = activeProductCount;
	}

	public long getInactiveProductCount() {
		return inactiveProductCount;
	}

	public void setInactiveProductCount(long inactiveProductCount) {
		this.inactiveProductCount = inactiveProductCount;
	}

	public long getAlmostExpiredCount() {
		return almostExpiredCount;
	}

	public void setAlmostExpiredCount(long almostExpiredCount) {
		this.almostExpiredCount = almostExpiredCount;
	}

	public List<ProductTemplate> getPtToExport() {
		return ptToExport;
	}

	public void setPtToExport(List<ProductTemplate> ptToExport) {
		this.ptToExport = ptToExport;
	}

}
