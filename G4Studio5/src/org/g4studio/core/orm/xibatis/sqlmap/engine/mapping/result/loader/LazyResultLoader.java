package org.g4studio.core.orm.xibatis.sqlmap.engine.mapping.result.loader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.g4studio.core.orm.xibatis.common.beans.ClassInfo;
import org.g4studio.core.orm.xibatis.sqlmap.engine.impl.SqlMapClientImpl;

/**
 * Class to lazily load results into objects
 */
public class LazyResultLoader implements InvocationHandler {

	private static final Class[] SET_INTERFACES = new Class[] { Set.class };
	private static final Class[] LIST_INTERFACES = new Class[] { List.class };

	protected SqlMapClientImpl client;
	protected String statementName;
	protected Object parameterObject;
	protected Class targetType;

	protected boolean loaded;
	protected Object resultObject;

	/**
	 * Constructor for a lazy list loader
	 * 
	 * @param client
	 *            - the client that is creating the lazy list
	 * @param statementName
	 *            - the statement to be used to build the list
	 * @param parameterObject
	 *            - the parameter object to be used to build the list
	 * @param targetType
	 *            - the type we are putting data into
	 */
	public LazyResultLoader(SqlMapClientImpl client, String statementName, Object parameterObject, Class targetType) {
		this.client = client;
		this.statementName = statementName;
		this.parameterObject = parameterObject;
		this.targetType = targetType;
	}

	/**
	 * Loads the result
	 * 
	 * @return the results - a list or object
	 * 
	 * @throws SQLException
	 *             if there is a problem
	 */
	public Object loadResult() throws SQLException {
		if (Collection.class.isAssignableFrom(targetType)) {
			InvocationHandler handler = new LazyResultLoader(client, statementName, parameterObject, targetType);
			ClassLoader cl = targetType.getClassLoader();
			if (Set.class.isAssignableFrom(targetType)) {
				return Proxy.newProxyInstance(cl, SET_INTERFACES, handler);
			} else {
				return Proxy.newProxyInstance(cl, LIST_INTERFACES, handler);
			}
		} else {
			return ResultLoader.getResult(client, statementName, parameterObject, targetType);
		}
	}

	public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
		if ("finalize".hashCode() == method.getName().hashCode() && "finalize".equals(method.getName())) {
			return null;
		} else {
			loadObject();
			if (resultObject != null) {
				try {
					return method.invoke(resultObject, objects);
				} catch (Throwable t) {
					throw ClassInfo.unwrapThrowable(t);
				}
			} else {
				return null;
			}
		}
	}

	private synchronized void loadObject() {
		if (!loaded) {
			try {
				loaded = true;
				resultObject = ResultLoader.getResult(client, statementName, parameterObject, targetType);
			} catch (SQLException e) {
				throw new RuntimeException("Error lazy loading result. Cause: " + e, e);
			}
		}
	}

}
