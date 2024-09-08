package net.ustyugov.jtalk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

public class MyEditText extends EditText {
	private String type;
	private String var;

	public MyEditText(Context context) {
		super(context);
	}
    public MyEditText(Context context, AttributeSet attr) {
        super(context, attr);
    }
	
	public void setType(String type) { this.type = type; }
	public void setVar(String var) { this.var = var; }
	public String getType() { return this.type; }
	public String getVar() { return this.var; }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions&EditorInfo.IME_MASK_ACTION;
        if ((imeActions&EditorInfo.IME_ACTION_SEND) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        return conn;
    }
}
