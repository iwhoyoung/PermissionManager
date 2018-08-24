# PermissionManagerDemo
在接口OnPermitListener中有3个方法，如下：<br>
1.该方法在查询权限时，返回被拒绝并不再提示的权限：
```java
protected void onActivityResult(int requestCode, int resultCode, Intent data)；
```
2.该方法在权限请求，用于完成选择后调用，参数为被拒绝的权限结果：
```java
public void onPermissionDenied(List<String> deniedPermission);
```
3.该方法在权限请求，且用户同意了所有方法后调用：
```java
public void onPermissionGranted();
```
PermissionManager为单例类，在需要请求权限的位置调用如下代码：
```java
PermissionManager.getInstance(this).oneKeyRequest();
```
重写onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)方法，加入代码：
```java
PermissionManager.getInstance(this).onPermissionResult(requestCode,permissions,grantResults);
```
重写onActivityResult(int requestCode, int resultCode, Intent data)方法，加入代码：
```java
PermissionManager.getInstance(this).onActivityResult(requestCode,resultCode,data);
```
Usage
===
Step 1. Add it in your root build.gradle at the end of repositories:
```java
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency
```java
	dependencies {
	        implementation 'com.github.iwhoyoung:PermissionManagerDemo:1.0.0'
	}
```
