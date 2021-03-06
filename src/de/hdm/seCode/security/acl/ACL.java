package de.hdm.seCode.security.acl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.hdm.seCode.security.SecureCallback;
import de.hdm.seCode.security.SecureInterface;
import de.hdm.seCode.security.acl.PermissionEntity.Scope;

public class ACL extends SecureCallback {
	private Map<Object, SecureInterface> secureInstanceList;
	private Map<Object, Object> globalObjectsList;
	private ArrayList<PermissionEntity> permissionList = new ArrayList<PermissionEntity>();
	private Map<Object,Object> ownerList;
	
	private static ACL acl;
	private ACL() {

		}
	public static ACL getInstance() {
		if (acl == null) {
			acl = new ACL();
			try {
				acl.parseACLFile();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return acl;
	}
	
	
	public Map<Object, Object> getGlobalObjectsList() {
		return globalObjectsList;
	}
	public ArrayList<PermissionEntity> getPermissionList() {
		return permissionList;
	}
	
	public Object getOwner(Object o) {
		return ownerList.get(o);
	}

	public Map<Object, SecureInterface> getSecureInterfacesList() {
		return secureInstanceList;
	}
	
	public void parseACLFile() throws ParserConfigurationException, SAXException, IOException {
		Map<String, Object> data = XML2ACLParser.getInstancesFromACLFile(new File("resource/acl.xml"));
		secureInstanceList = (Map<Object, SecureInterface>) data.get("secureInterfaces");
		globalObjectsList = (Map<Object, Object>) data.get("global_objects");
		ownerList = (Map<Object, Object>) data.get("owner_objects");
		try {
			permissionList = XML2ACLParser.getPermissionsFromACLFile(new File("resource/acl.xml"), data);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public boolean checkPermission(Method m, Object callee, Object caller) {
		return checkPermission(m.getName(), callee, caller);
	}
	public boolean removePermission(PermissionEntity entity, Integer tan, SecureCallback caller){
		if(caller.getTan().equals(tan)){
			for(SecureInterface target: entity.getTargetInstances()){
				if(!target.isOwner(caller,this, createTan())){
					return false;
				}
				return permissionList.remove(entity);
			}
		}
		return false;
		
	}

	private Set<PermissionEntity> findPermissionWithRawObject(String method, Object callee, List<PermissionEntity> permissionList){
		Set<PermissionEntity> result = new HashSet<PermissionEntity>();
		for(PermissionEntity entity: permissionList){
			if(entity.getTargetScope()==Scope.INSTANCE){
				if(secureInterfaces2Objects(entity.getTargetInstances()).contains(callee)){
					if(entity.isAllMethods()){
						result.add(entity);
					} else if(entity.getMethods().contains(method)){
						result.add(entity);
					}
				}
			} 
		}
		return result;
	}
	
	private Set<Object> secureInterfaces2Objects(List<SecureInterface> secureInterfaces){
		Set<Object> objects = new HashSet<Object>();
		for(SecureInterface si:secureInterfaces){
			objects.add(si.getObject(this, createTan()));
		}
		return objects;
	}

	private Set<PermissionEntity> findPermission(String method, Object callee, List<PermissionEntity> permissionList){
		Set<PermissionEntity> result = findPermissionWithRawObject( method, callee, permissionList);
		for(PermissionEntity entity: permissionList){
			if(entity.getTargetScope() == Scope.INSTANCE){
				if(entity.getTargetInstances().contains(callee)){
					if(entity.isAllMethods()){
						result.add(entity);
					} else if(entity.getMethods().contains(method)){
						result.add(entity);
					}
				}
			} else if(entity.getTargetScope() == Scope.CLASS){
				if(entity.getTargetClass().equals(callee.getClass())){
					if(entity.isAllMethods()){
						result.add(entity);
					} else if(entity.getMethods().contains(method)){
						result.add(entity);
					}
				}
			}
		}
		return result;
	}
	
	public boolean checkPermission(String method, Object callee, Object caller) {
		Set<PermissionEntity> permissions = findPermission(method, callee, permissionList);
		for(PermissionEntity entity:permissions){
			if(entity.getCallerScope() == Scope.INSTANCE){
				if(entity.getCallerInstances().contains(caller)) return true;
				else if (checkProxyContains(entity.getCallerInstances(),caller)) return true;
			}
			else if(entity.getCallerScope() == Scope.CLASS){
				if(entity.getCallerClass().equals(caller.getClass())) return true;
			}
		}
		return false;
		

	}

	private boolean checkProxyContains(List<Object> callerInstances,
			Object caller) {
		for(Object instance: callerInstances){
			if(instance instanceof SecureInterface){
				if(((SecureInterface)instance).getObject(this, createTan()).equals(caller)) return true;
			}
		}
		return false;
	}
	public boolean addPermission(PermissionEntity entity, Integer tan, SecureCallback caller) {
		if(tan.equals(caller.getTan())){
			for(SecureInterface target: entity.getTargetInstances()){
				if(!target.isOwner(caller,this, createTan())){
					return false;
				}
			}
			permissionList.add(entity);
			return true;
		}
		return false;
	}


	
}
