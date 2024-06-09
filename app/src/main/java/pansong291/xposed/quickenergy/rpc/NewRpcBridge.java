package pansong291.xposed.quickenergy.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;
import pansong291.xposed.quickenergy.entity.RpcEntity;
import pansong291.xposed.quickenergy.hook.ApplicationHook;
import pansong291.xposed.quickenergy.hook.Notification;
import pansong291.xposed.quickenergy.util.ClassUtil;
import pansong291.xposed.quickenergy.util.Config;
import pansong291.xposed.quickenergy.util.Log;
import pansong291.xposed.quickenergy.util.RandomUtils;

/**
 * 新版rpc接口 支持最低支付宝版本v10.3.96.8100
 * 记录rpc抓包 支持最低支付宝版本v10.3.96.8100
 */
public class NewRpcBridge implements RpcBridge {

    private static final String TAG = NewRpcBridge.class.getSimpleName();

    private ClassLoader loader;

    private Object newRpcInstance;

    private Method parseObjectMethod;

    private Class<?>[] bridgeCallbackClazzArray;

    private Method newRpcCallMethod;


    @Override
    public void load() {
        loader = ApplicationHook.getClassLoader();
        try {
            Object service = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", loader), "getService");
            Object extensionManager = XposedHelpers.callMethod(service, "getExtensionManager");
            Method getExtensionByName = extensionManager.getClass().getDeclaredMethod("createExtensionInstance", Class.class);
            getExtensionByName.setAccessible(true);
            newRpcInstance = getExtensionByName.invoke(null, loader.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"));
            if (newRpcInstance == null) {
                Object nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap");
                if (nodeExtensionMap != null) {
                    Map<Object, Map<String, Object>> map = (Map<Object, Map<String, Object>>) nodeExtensionMap;
                    for (Map.Entry<Object, Map<String, Object>> entry : map.entrySet()) {
                        Map<String, Object> map1 = entry.getValue();
                        for (Map.Entry<String, Object> entry1 : map1.entrySet()) {
                            if ("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension".equals(entry1.getKey())) {
                                newRpcInstance = entry1.getValue();
                                break;
                            }
                        }
                    }
                }
                if (newRpcInstance == null) {
                    Log.i(TAG, "get newRpcInstance null");
                    throw new RuntimeException("get newRpcInstance is null");
                }
            }
            parseObjectMethod = loader.loadClass("com.alibaba.fastjson.JSON").getMethod("parseObject", String.class);
            Class<?> bridgeCallbackClazz = loader.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback");
            bridgeCallbackClazzArray = new Class[]{bridgeCallbackClazz};
            newRpcCallMethod = newRpcInstance.getClass().getMethod("rpc"
                    , String.class
                    , boolean.class
                    , boolean.class
                    , String.class
                    , loader.loadClass(ClassUtil.JSON_OBJECT_NAME)
                    , String.class
                    , loader.loadClass(ClassUtil.JSON_OBJECT_NAME)
                    , boolean.class
                    , boolean.class
                    , int.class
                    , boolean.class
                    , String.class
                    , loader.loadClass("com.alibaba.ariver.app.api.App")
                    , loader.loadClass("com.alibaba.ariver.app.api.Page")
                    , loader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext")
                    , bridgeCallbackClazz
            );
            Log.i(TAG, "get newRpcCallMethod successfully");
        } catch (Exception e) {
            Log.i(TAG, "get newRpcCallMethod err:");
            Log.printStackTrace(TAG, e);
        }
    }

    @Override
    public void unload() {
        newRpcCallMethod = null;
        bridgeCallbackClazzArray = null;
        parseObjectMethod = null;
        newRpcInstance = null;
        loader = null;
    }

    public String requestString(String method, String data, int retryCount) {
        RpcEntity rpcEntity = requestObject(method, data, retryCount);
        if (rpcEntity != null) {
            return rpcEntity.getResultStr();
        }
        return null;
    }

    @Override
    public RpcEntity requestObject(String method, String data, int retryCount) {
        if (ApplicationHook.isOffline()) {
            return null;
        }
        int count = 0;
        do {
            count++;
            RpcEntity rpcEntity = new RpcEntity();
            try {
                Log.i(TAG, "new rpc request: " + method + ", " + data);
                newRpcCallMethod.invoke(
                        newRpcInstance, method, false, false, "json", parseObjectMethod.invoke(null, "{\"__apiCallStartTime\":" + System.currentTimeMillis() + ",\"apiCallLink\":\"XRiverNotFound\",\"execEngine\":\"XRiver\",\"operationType\":\"" + method + "\",\"requestData\":" + data + "}"), "", null, true, false, 0, false, "", null, null, null, Proxy.newProxyInstance(loader, bridgeCallbackClazzArray, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) {
                                if(args.length == 1 && "sendJSONResponse".equals(method.getName())) {
                                    try {
                                        Object obj = args[0];
                                        if (!(Boolean) XposedHelpers.callMethod(obj, "containsKey", "success")) {
                                            rpcEntity.setError();
                                        }
                                        String result = (String) XposedHelpers.callMethod(obj, "toJSONString");
                                        rpcEntity.setResult(obj, result);
                                        Log.i(TAG, "new rpc response: " + result);
                                    } catch (Exception e) {
                                        rpcEntity.setError();
                                        Log.i(TAG, "new rpc response [" + method + "] err:");
                                        Log.printStackTrace(TAG, e);
                                    }
                                }
                                return null;
                            }
                        })
                );
                if (!rpcEntity.getHasResult()) {
                    return null;
                }
                if (!rpcEntity.getHasError()) {
                    return rpcEntity;
                }
                try {
                    String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResult(), "getString", "error");
                    if ("2000".equals(errorCode)) {
                        if (!ApplicationHook.isOffline()) {
                            ApplicationHook.setOffline(true);
                            Notification.setContentText("登录超时");
                            if (Config.INSTANCE.isTimeoutRestart()) {
                                Log.record("尝试重新登录");
                                ApplicationHook.reLoginByBroadcast();
                            }
                        }
                        return null;
                    }
                    return rpcEntity;
                } catch (Exception e) {
                    Log.i(TAG, "new rpc response [" + method + "] get err:");
                    Log.printStackTrace(TAG, e);
                }
                try {
                    Thread.sleep(600 + RandomUtils.delay());
                } catch (InterruptedException e) {
                    Log.printStackTrace(e);
                }
            } catch (Throwable t) {
                Log.i(TAG, "new rpc request [" + method + "] err:");
                Log.printStackTrace(TAG, t);
                try {
                    Thread.sleep(600 + RandomUtils.delay());
                } catch (InterruptedException e) {
                    Log.printStackTrace(e);
                }
            }
        } while (count < retryCount);
        return null;
    }

    public RpcEntity newAsyncRequest(String method, String data, int retryCount) {
        if (ApplicationHook.isOffline()) {
            return null;
        }
        int count = 0;
        do {
            count++;
            RpcEntity rpcEntity = new RpcEntity(Thread.currentThread());
            Long id = rpcEntity.getId();
            try {
                synchronized (id) {
                    Log.i(TAG, "new rpc request: " + method + ", " + data);
                    newRpcCallMethod.invoke(
                            newRpcInstance, method, false, false, "json", parseObjectMethod.invoke(null, "{\"__apiCallStartTime\":" + System.currentTimeMillis() + ",\"apiCallLink\":\"XRiverNotFound\",\"execEngine\":\"XRiver\",\"operationType\":\"" + method + "\",\"requestData\":" + data + "}"), "", null, true, false, 0, false, "", null, null, null, Proxy.newProxyInstance(loader, bridgeCallbackClazzArray, new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) {
                                    if(args.length == 1 && "sendJSONResponse".equals(method.getName())) {
                                        try {
                                            synchronized (id) {
                                                Object obj = args[0];
                                                if (!(Boolean) XposedHelpers.callMethod(obj, "containsKey", "success")) {
                                                    rpcEntity.setError();
                                                }
                                                String result = (String) XposedHelpers.callMethod(obj, "toJSONString");
                                                rpcEntity.setResult(obj, result);
                                                Log.i(TAG, "new rpc response: " + result);
                                                Thread thread = rpcEntity.getThread();
                                                if (thread != null) {
                                                    id.notifyAll();
                                                }
                                            }
                                        } catch (Exception e) {
                                            rpcEntity.setError();
                                            Log.i(TAG, "new rpc response [" + method + "] err:");
                                            Log.printStackTrace(TAG, e);
                                            synchronized (id) {
                                                Thread thread = rpcEntity.getThread();
                                                if (thread != null) {
                                                    id.notifyAll();
                                                }
                                            }
                                        }
                                    }
                                    return null;
                                }
                            })
                    );
                    id.wait(30_000);
                }
                if (!rpcEntity.getHasResult()) {
                    return null;
                }
                if (!rpcEntity.getHasError()) {
                    return rpcEntity;
                }
                try {
                    String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResult(), "getString", "error");
                    if ("2000".equals(errorCode)) {
                        if (!ApplicationHook.isOffline()) {
                            ApplicationHook.setOffline(true);
                            Notification.setContentText("登录超时");
                            if (Config.INSTANCE.isTimeoutRestart()) {
                                Log.record("尝试重新登录");
                                ApplicationHook.reLoginByBroadcast();
                            }
                        }
                        return null;
                    }
                    return rpcEntity;
                } catch (Exception e) {
                    Log.i(TAG, "new rpc response [" + method + "] get err:");
                    Log.printStackTrace(TAG, e);
                }
                try {
                    Thread.sleep(600 + RandomUtils.delay());
                } catch (InterruptedException e) {
                    Log.printStackTrace(e);
                }
            } catch (Throwable t) {
                Log.i(TAG, "new rpc request [" + method + "] err:");
                Log.printStackTrace(TAG, t);
                try {
                    Thread.sleep(600 + RandomUtils.delay());
                } catch (InterruptedException e) {
                    Log.printStackTrace(e);
                }
            } finally {
                rpcEntity.delThread();
            }
        } while (count < retryCount);
        return null;
    }

}