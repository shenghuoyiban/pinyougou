package entity;

import java.io.Serializable;

public class Result implements Serializable{
    private String message;//返回的信息
    private boolean success;//是否成功
	public String getMessage() {
		return message;
	}
	public void setMessage(Boolean success,String message) {
		this.message = message;
		this.success = success;
	}
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
		this.success = success;
	}
	@Override
	public String toString() {
		return "Result [message=" + message + ", success=" + success + "]";
	}
	public Result( Boolean success,String message) {
		super();
		this.success = success;
		this.message = message;
		
	}
    
}
