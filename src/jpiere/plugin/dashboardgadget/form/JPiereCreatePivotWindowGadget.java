/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package jpiere.plugin.dashboardgadget.form;


import java.util.HashMap;
import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.dashboard.DashboardPanel;
import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MForm;
import org.compiere.model.MRole;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Box;
import org.zkoss.zul.Vbox;


/**
 *  JPIERE-0359
 *  JPiere Plugins(JPPS) Dashboard Gadget Create Pivot Window Gadget
 *
 *  @author Hideaki Hagiwara（h.hagiwara@oss-erp.co.jp）
 *
 */
public class JPiereCreatePivotWindowGadget extends DashboardPanel  implements EventListener<Event> {


	public JPiereCreatePivotWindowGadget()
	{
		super();
		setSclass("views-box");
		this.appendChild(createPivotWindowGadgetPanel());
	}

	HashMap<Integer, Integer> pv_form_map = new HashMap<Integer, Integer>();
			
	private Box createPivotWindowGadgetPanel()
	{
		MRole role = MRole.getDefault();
		
		List<MForm> formList = new Query(Env.getCtx(), "AD_Form", "Classname like 'JP_PivotWindow_ID=%'", null)
				.setOnlyActiveRecords(true)
				.list();
		
		for(MForm form : formList)
		{
			if(role.getFormAccess(form.getAD_Form_ID()))
				pv_form_map.put(Integer.valueOf(form.getClassname().substring("JP_PivotWindow_ID=".length())), form.getAD_Form_ID() );
		}
		
		List<PO> pivotWindowList = new Query(Env.getCtx(), "JP_PivotWindow", "IsValid='Y' AND IsShowInDashboard='Y'", null)
		.setOnlyActiveRecords(true)
		.setOrderBy("SeqNo")
		.list();
		
		PO[] pivots = pivotWindowList.toArray(new PO[pivotWindowList.size()]);
		int JP_PivotWindow_ID = 0;
		Vbox vbox = new Vbox();
		for (int i = 0; i < pivots.length; i++)
		{
			PO pivot = pivots[i];
			JP_PivotWindow_ID = pivot.get_ValueAsInt("JP_PivotWindow_ID");
			if(JP_PivotWindow_ID > 0 && role.getFormAccess(pv_form_map.get(JP_PivotWindow_ID)))
			{
				ToolBarButton btn = new ToolBarButton(pivot.get_ValueAsString("Name"));
				btn.setSclass("link");
				btn.setLabel(pivot.get_Translation("Name"));
				btn.setImage(ThemeManager.getThemeResource("images/" + (Util.isEmpty(pivot.get_ValueAsString("ImageURL")) ? "Info16.png" : pivot.get_Value("ImageURL"))));
				btn.addEventListener(Events.ON_CLICK, this);
				vbox.appendChild(btn);
			}
		}

		return vbox;
	
	}
	

	@Override
	public void onEvent(Event event) throws Exception 
	{
		Component comp = event.getTarget();
		String eventName = event.getName();
		if(eventName.equals(Events.ON_CLICK))
		{
			if(comp instanceof ToolBarButton)
			{
				//Check Plugin of Pivot Window exist
				boolean hasPivotWindow = false;
				List<IFormFactory> factories = Service.locator().list(IFormFactory.class).getServices();
				if (factories != null) 
				{
					for(IFormFactory factory : factories) 
					{
						if(factory.toString().equals("PivotWindow"))
						{
							hasPivotWindow = true;
							break;
						}
					}
				}
				
				if(hasPivotWindow)
				{
				
					ToolBarButton btn = (ToolBarButton) comp;
					String actionCommand = btn.getName();
	
					int JP_PivotWindow_ID = new Query(Env.getCtx(), "JP_PivotWindow", "Name = ?", null)
					.setParameters(actionCommand)
					.setOnlyActiveRecords(true)
					.firstIdOnly();
	
					if (JP_PivotWindow_ID<=0)
						return;
					
					SessionManager.getAppDesktop().openForm(pv_form_map.get(JP_PivotWindow_ID));
					
				}else{
					//TODO ピボットウィンドウの案内パネル作成
					FDialog.info(0, this, "Saved", Msg.getMsg(Env.getCtx(), "Error"), Msg.getElement(Env.getCtx(), "JP_PivotWindow_ID") );
				}
			}

		}
	}


}