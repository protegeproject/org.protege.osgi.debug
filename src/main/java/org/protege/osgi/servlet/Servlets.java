package org.protege.osgi.servlet;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.protege.osgi.PackageViewer;

public class Servlets implements PackageViewer {
    private Map<HttpServlet, String> servletNameMap = new HashMap<HttpServlet, String>();
    
    public void initialize(final BundleContext context) 
    throws InvalidSyntaxException, ServletException, NamespaceException {
        servletNameMap.put(new MainServlet(), MainServlet.PATH);
        servletNameMap.put(new ClassLoaderServlet(context), ClassLoaderServlet.PATH);
        servletNameMap.put(new PackageServlet(context), PackageServlet.PATH);
        servletNameMap.put(new LoggingServlet(), LoggingServlet.PATH);
        Collection<ServiceReference<HttpService>> serviceReferences = context.getServiceReferences(HttpService.class, null);
        if (serviceReferences != null) {
            for (ServiceReference<HttpService> sr : serviceReferences) {
                HttpService service = context.getService(sr);
                registerServlets(service);
            }
        }
        context.addServiceListener(new ServiceListener() {

            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    try {
                        ServiceReference sr = event.getServiceReference();
                        Object o = context.getService(sr);
                        if (o != null && o instanceof HttpService) {
                            registerServlets((HttpService) o);
                        }
                    }
                    catch (NamespaceException e) {
                        e.printStackTrace();
                    }
                    catch (ServletException e) {
                        e.printStackTrace();
                    }
                }
            }
            
        }, "(" +  Constants.OBJECTCLASS + "=" + HttpService.class.getName() + ")");

    }
    
    public void dispose() {
        servletNameMap.clear();
    }
    
    private void registerServlets(HttpService httpService) throws ServletException, NamespaceException {
        if (httpService != null)  {
            Dictionary<String, String> params = new Hashtable<String, String>();
            for (Map.Entry<HttpServlet, String> entry : servletNameMap.entrySet()) {
                httpService.registerServlet(entry.getValue(), entry.getKey(), params, null);
            }
        }
    }

}
