/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.freiraum.server;
import java.util.Collection;
import java.util.Locale;

import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.logger.Logger;
import org.rapla.plugin.freiraum.FreiraumPlugin;
import org.rapla.plugin.freiraum.common.RaplaJsonService;
import org.rapla.server.RaplaServerExtensionPoints;
import org.rapla.server.ServerServiceContainer;

/**
   This is a demonstration of a rapla-plugin. It adds a sample usecase and option
   to the rapla-system.
 */

public class FreiraumServerPlugin implements PluginDescriptor<ServerServiceContainer>
{
    Logger logger;

    public FreiraumServerPlugin(Logger logger) {
        this.logger = logger;
    }
   
    public Logger getLogger() {
        return logger;
    }

    /**
     * @throws RaplaContextException 
     * @see org.rapla.framework.PluginDescriptor
     */
    public void provideServices(ServerServiceContainer container, Configuration config) throws RaplaContextException {
    	// add freiraum even if config not set
        if ( !config.getAttributeAsBoolean("enabled", true) )
        	return;

    	container.addResourceFile(FreiraumPlugin.RESOURCE_FILE);
    	container.addWebpage("freiraum-ajax",FreiraumExportPageGenerator.class, config  );
		container.addRemoteMethodFactory(RaplaJsonService.class, RaplaJsonServiceFactory.class,config);
    	//container.addContainerProvidedComponent( RaplaServerExtensionPoints.HTML_MAIN_MENU_EXTENSION_POINT, FreiraumMenuEntry.class);
    	//container.addWebpage("freiraum-push",FreiraumPushPageGenerator.class, config  );
    	//container.addWebpage("freiraum-poll",FreiraumPollPageGenerator.class, config  );
    	
//        try {
//            RaplaResourcePageGenerator resourcePageGenerator = container.getContext().lookup(RaplaResourcePageGenerator.class);
//            // registers the standard calendar files
//            
//            URL resource = this.getClass().getResource("/org/rapla/plugin/dhbwterminal/kursuebersicht.css");
//			resourcePageGenerator.registerResource( "kursuebersicht.css", "text/css", resource);
//
//            resource = this.getClass().getResource("/org/rapla/plugin/dhbwterminal/kursuebersicht2.css");
//            resourcePageGenerator.registerResource( "kursuebersicht2.css", "text/css", resource);
//
//            resource = this.getClass().getResource("/org/rapla/plugin/dhbwterminal/kursuebersicht3.css");
//            resourcePageGenerator.registerResource( "kursuebersicht3.css", "text/css", resource);
//
//
//        } catch ( Exception ex) {
//        	getLogger().error("Could not initialize terminal plugin on server" , ex);
//        }
//    	ClientFacade facade = container.getContext().lookup(ClientFacade.class);
//    	try {
//            convertOldDhbwData(facade);
//        } catch (RaplaException e) {
//            throw new RaplaContextException( e.getMessage(), e);
//        }
    }
    
    
    public void convertOldDhbwData(ClientFacade facade) throws RaplaException {
        DynamicType dynamicType;
        try
        {
            dynamicType = facade.getDynamicType("raum");
            Allocatable[] allocatables = facade.getAllocatables();
            for (Allocatable allocatable:allocatables)
            {
                Classification classification = allocatable.getClassification();
                Attribute raumAtt = classification.getAttribute("raum");
                String newRaumKey = "raumnummer";
                Attribute raumAttNew = classification.getAttribute(newRaumKey);
                if ( raumAtt != null && raumAttNew != null)
                {
                    Object newValue = classification.getValue( raumAttNew ); 
                    Object value = classification.getValue(raumAtt);
                    if ( newValue == null && value != null && value instanceof Category)
                    {
                        if ( classification.getType().getKey().equals("raum"))
                        {
                            Allocatable editable = facade.edit( allocatable);
                            String roomName = getRoomName(value);
                            editable.getClassification().setValue(newRaumKey , roomName);
                            facade.store( editable);
                            getLogger().info("Updating resoure to new room name " + editable);
                        }
                        else
                        {
                            // String raumKey = ((Category)value).getKey();
                            ClassificationFilter filter = dynamicType.newClassificationFilter();
                            filter.addEqualsRule("raum",value);
                            Allocatable[] foundRooms = facade.getAllocatables( filter.toArray());
                            Allocatable room;
                            if ( foundRooms.length>0)
                            {
                                room = foundRooms[0];
                            }
                            else
                            {
                                Classification newClassification = dynamicType.newClassification(true);
                                String roomName = getRoomName(value);
                                newClassification.setValue("raum", roomName);
                                getLogger().info("Creating resource " + roomName);
                                room  = facade.newAllocatable(newClassification, null);
                                Collection<Permission> permissions = room.getPermissionList();
                                if ( permissions.size() > 0)
                                {
                                    permissions.iterator().next().setAccessLevel(Permission.READ);
                                }
                                else
                                {
                                    Permission permission = room.newPermission();
                                    permission.setAccessLevel( Permission.READ);
                                    room.addPermission( permission );
                                }
                                facade.store( room );
                            }
                            Allocatable editable = facade.edit( allocatable);
                            editable.getClassification().setValue(newRaumKey , room);
                            facade.store( editable);
                            getLogger().info("Updating resoure to new room link " + editable);
                        }
                    }
                }
            }
        } catch (EntityNotFoundException ex)
        {
        }
    }

    private String getRoomName(Object value) {
        Locale locale = Locale.GERMANY;
        if (value instanceof Category)
        {
            Category category = (Category) value;
            Category parent = category.getParent();
            StringBuilder buf = new StringBuilder();
            buf.append(parent != null ? parent.getName(locale) : "").append(category.getKey().replace((parent != null ? parent.getName(locale) : ""), ""));
            return buf.toString();
        }
        else
        {
           
            String string = value.toString();
            return string;
        }
    }

}

