/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.ajax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.ajax.json.JsonFunction;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * A behavior that collects the Ajax Request attributes for all AjaxEventBehaviors
 * on the same event in the children components of the host component.
 * This way there is only one JavaScript event binding and all the Ajax call listeners are preserved.
 */
public class EventDelegatingBehavior extends AjaxEventBehavior
{
	private Map<String, CharSequence> attrsMap;

	private boolean initialized = false;

	/**
	 * Construct.
	 *
	 * @param event the event this behavior will be attached to
	 */
	public EventDelegatingBehavior(String event)
	{
		super(event);
	}

	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);

		if (initialized == false)
		{
			Page page = component.getPage();
			HashSet<String> enabledEvents = page.getMetaData(EVENT_NAME_PAGE_KEY);
			if (enabledEvents == null)
			{
				enabledEvents = new HashSet<>();
				page.setMetaData(EVENT_NAME_PAGE_KEY, enabledEvents);
			}
			enabledEvents.add(getEvent());
			initialized = true;
		}
	}

	/**
	 * Collects the Ajax Request Attributes for a AjaxEventBehavior in a child component
	 * @param componentId
	 * @param attributes
	 */
	protected final void contributeComponentAttributes(String componentId, CharSequence attributes)
	{
		if (attrsMap == null)
		{
			attrsMap = new HashMap<>();
		}
		attrsMap.put(componentId, new JsonFunction(attributes));
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		attrsMap = null;
	}

	@Override
	protected CharSequence getCallbackScript(Component component)
	{
		JSONObject attributesMap = new JSONObject(attrsMap);

		return String.format("Wicket.Event.delegate('%s', '%s', %s)",
				component.getMarkupId(), getEvent(), attributesMap.toString());
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);

		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(EventDelegatingBehavior.class, "res/js/EventDelegatingBehavior.js")
		{
			@Override
			public List<HeaderItem> getDependencies()
			{
				List<HeaderItem> dependencies = super.getDependencies();
				dependencies.add(JavaScriptHeaderItem.forReference(Application.get().getJavaScriptLibrarySettings().getWicketEventReference()));
				return dependencies;
			}
		}));
	}

	@Override
	protected final void onEvent(AjaxRequestTarget target)
	{
		throw new UnsupportedOperationException("Executed the event delegating behavior should never happen");
	}
}