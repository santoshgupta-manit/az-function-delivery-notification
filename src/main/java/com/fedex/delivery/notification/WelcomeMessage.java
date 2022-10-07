package com.fedex.delivery.notification;

public class WelcomeMessage {
	
	private String welcomeMessage;
	
	private String buttonText;

	public String getWelcomeMessage() {
		return welcomeMessage;
	}

	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}

	public String getButtonText() {
		return buttonText;
	}

	public void setButtonText(String buttonText) {
		this.buttonText = buttonText;
	}

	@Override
	public String toString() {
		return "WelcomeMessage [welcomeMessage=" + welcomeMessage + ", buttonText=" + buttonText + "]";
	}
	
	

}
