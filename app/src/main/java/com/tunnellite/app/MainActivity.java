package com.tunnellite.app;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends Activity {
 private static final int PICK_PROFILE = 41;
 private TextView status;
 private File currentProfile;

 @Override public void onCreate(Bundle state) { super.onCreate(state); buildScreen(); }
 private void buildScreen() {
  LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(28,42,28,28); root.setBackgroundColor(0xFF0B1220);
  TextView brand = text("TUNNEL LITE",13,0xFF55D6BE); brand.setLetterSpacing(.18f); root.addView(brand);
  TextView title = text("Your connection.\nYour route.",32,0xFFFFFFFF); title.setPadding(0,24,0,12); root.addView(title);
  TextView desc = text("Import a VPN profile from a provider you trust, then open it in a compatible OpenVPN app.",15,0xFFB8C4D6); root.addView(desc);
  TextView label = text("PROFILE STATUS",12,0xFF55D6BE); label.setPadding(18,20,18,8); label.setBackgroundColor(0xFF172338);
  LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=36; root.addView(label,lp);
  status = text("No profile imported",16,0xFFFFFFFF); status.setPadding(18,8,18,22); status.setBackgroundColor(0xFF172338); root.addView(status);
  Button imp = new Button(this); imp.setText("IMPORT .OVPN PROFILE"); imp.setOnClickListener(v -> pickProfile()); lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=28; root.addView(imp,lp);
  Button open = new Button(this); open.setText("OPEN IN VPN CLIENT"); open.setOnClickListener(v -> openProfile()); root.addView(open);
  TextView note = text("IMPORTANT\nTunnel Lite does not provide free mobile data or VPN servers. You still need an internet connection, a valid VPN profile and a compatible VPN client. Never import profiles from sources you do not trust.",13,0xFF9EADC2); note.setPadding(4,28,4,0); root.addView(note);
  TextView footer=text("PRIVATE PROFILE STORAGE  •  NO ACCOUNT REQUIRED",10,0xFF60738F); footer.setGravity(Gravity.CENTER); lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=30; root.addView(footer,lp);
  setContentView(root);
 }
 private TextView text(String s,float size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
 private void pickProfile() { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/x-openvpn-profile","application/octet-stream","text/plain"}); startActivityForResult(i,PICK_PROFILE); }
 @Override protected void onActivityResult(int req,int res,Intent data) {
  super.onActivityResult(req,res,data); if(req!=PICK_PROFILE || res!=RESULT_OK || data==null || data.getData()==null)return;
  try(InputStream in=getContentResolver().openInputStream(data.getData())) {
   File dir=new File(getCacheDir(),"profiles"); if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create profile folder");
   File dest=new File(dir,"selected-profile.ovpn");
   try(FileOutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n);}
   currentProfile=dest; status.setText("Profile imported successfully.\nReady to open in a VPN client."); Toast.makeText(this,"VPN profile imported",Toast.LENGTH_SHORT).show();
  } catch(Exception e){Toast.makeText(this,"Import failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 private void openProfile() {
  if(currentProfile==null||!currentProfile.exists()){Toast.makeText(this,"Import a .ovpn profile first",Toast.LENGTH_SHORT).show();return;}
  try { Uri uri=FileProvider.getUriForFile(this,getPackageName()+".provider",currentProfile); Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(uri,"application/x-openvpn-profile"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(Intent.createChooser(i,"Open VPN profile with")); }
  catch(Exception e){Toast.makeText(this,"Install a compatible OpenVPN client first.",Toast.LENGTH_LONG).show();}
 }
}
