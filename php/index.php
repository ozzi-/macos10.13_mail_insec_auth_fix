<?php
  // What you need to do to get this running:
  // Pick a random key for your own instance if you will use this in production -> $key
  // Get some blobs named port_blob_imap.txt port_blob_smtp.txt, containing the according port numbers (see examples)

  if (!file_exists('updir')) {
      mkdir('updir', 0740, true);
  }

  $step = "index";
  if(isset($_GET["step"])){
    $step = $_GET["step"];
  }
  if($step==="index"){?>
    <form action="index.php?step=upload" method="post" enctype="multipart/form-data">
      Select image to upload:
      <input type="file" name="fileToUpload" id="fileToUpload">
      <input type="submit" value="Upload Accounts4" name="submit">
    </form>
  <?php }
  if($step==="upload"){
    $target_dir = "updir/";
    $date = date_create();
    $date = date_timestamp_get($date);
    $filename = encrypt($date.uniqid().basename($_FILES["fileToUpload"]["name"]));
    $filename = b64urlsafe($filename);
    $target_file = $target_dir . $filename;
    $uploadOk = 1;
    $fileType = $_FILES["fileToUpload"]["name"];
    if(isset($_POST["submit"])) {
        $uploadOk = 1;
    }
    if ($_FILES["fileToUpload"]["size"] > 500000) {
      echo "Sorry, your file is too large.";
      $uploadOk = 0;
    }
    if(!endsWith($fileType,"sqlite")){
      $uploadOk = 0;
      echo "Invalid file type";
    }
    if ($uploadOk == 1 && move_uploaded_file($_FILES["fileToUpload"]["tmp_name"], $target_file)) {
        header("Location: index.php?step=parse&id=".$filename);
        die();
    } else {
        echo "Sorry, there was an error uploading your file.";
    }
  }

  if($step==="parse" && isset($_GET["id"])){

      $db = openDB();

      $ret = $db->query("SELECT * FROM ZACCOUNT");
      if(!$ret){
        die("Error reading account DB");
      }
      $accounts = array();
      while($row = $ret->fetchArray(SQLITE3_ASSOC) ) {
        array_push($accounts, $row['ZACCOUNTDESCRIPTION']);
      }
      $db->close();
      $accounts = array_unique($accounts);
      ?>Please choose account you wish to fix:<br><?php
      foreach($accounts as &$account){?>
        <a href="index.php?step=parse2&id=<?= $_GET["id"]?>&acc=<?= $account ?>"><?= $account ?></a><br>
        <?php
      }
  }
  if($step==="parse2" && isset($_GET["id"]) && isset($_GET["acc"])){
    $db = openDB();
    $retZACC = $db->query("SELECT * FROM ZACCOUNT WHERE ZACCOUNTDESCRIPTION='".$_GET["acc"]."'");
    if(!$retZACC){
       die("Error reading account DB");
    }
    while($rowZACC = $retZACC->fetchArray(SQLITE3_ASSOC)){
      $retp = $db->query("SELECT ZACCOUNTTYPEDESCRIPTION FROM ZACCOUNTTYPE WHERE Z_PK = '".$rowZACC["ZACCOUNTTYPE"]."';");
      while($row = $retp->fetchArray(SQLITE3_ASSOC)){
      	$id = array (
      		"desc" => $row["ZACCOUNTTYPEDESCRIPTION"],
      		"pk" => $rowZACC["Z_PK"]
      	);
      	// ********
      	// * PORT *
      	// ********
      	$db->exec("DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='PortNumber' AND ZOWNER = '".$id["pk"]."';");
              $blobPort="";
              if($id["desc"]=="IMAP"){
                $blobPort= file_get_contents("port_blob_imap.txt");
              }else{
                $blobPort= file_get_contents("port_blob_smtp.txt");
      	}
      	$stmt = $db->prepare("INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES ('3','1','".$id["pk"]."','PortNumber',?)");
      	$stmt->bindValue(1,$blobPort,SQLITE3_BLOB);
      	$stmt->execute();
      	// ***********
      	// * DynConf *
      	// ***********
      	 $db->exec("DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='DisableDynamicConfiguration' AND ZOWNER = '".$id["pk"]."';");
              $blobBoolTrue = file_get_contents("blob_bool_true.txt");
      	 $stmt = $db->prepare("INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES ('3','1','".$id["pk"]."','DisableDynamicConfiguration',?)");
      	 $stmt->bindValue(1,$blobPort,SQLITE3_BLOB);
      	 $stmt->execute();
      	// **************
      	// * AllowInsec *
      	// **************
    	 $db->exec("DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='AllowsInsecureAuthentication' AND ZOWNER = '".$id["pk"]."';");
	     $stmt = $db->prepare("INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES ('3','1','".$id["pk"]."','AllowsInsecureAuthentication',?)");
	     $stmt->bindValue(1,$blobPort,SQLITE3_BLOB);
	     $stmt->execute();
      }
    }
    $db->close();
    header("Location: index.php?step=download&id=".$_GET["id"]);
    die();
  }

  if($step==="download" && isset($_GET["id"])){
    if(!preg_match("/^[0-9a-zA-Z-]*$/",$_GET["id"])){
      die("");
    }
    $fp = "updir/".$_GET["id"];
    if(file_exists($fp)){
      if(isset($_GET["download"])){
        header("Content-Type: application/octet-stream");
        header("Content-Transfer-Encoding: Binary");
        header("Content-disposition: attachment; filename=\"Accounts4.sqlite\"");
        readfile($fp);
	die();
      }else{
        ?><a href="index.php?step=download&id=<?= $_GET['id'] ?>&download=force">Download fixed file</a><?php
      }
    }
  }

  function openDB(){
    if(!preg_match("/^[0-9a-zA-Z-]*$/",$_GET["id"])){
      die("");
    }
    $fp = "updir/".$_GET["id"];
    if(file_exists($fp)){
      class MyDB extends SQLite3 {
        function __construct($fp) {
          $this->open($fp);
        }
      }
      $db = new MyDB($fp);
      if(!$db){
        die("Corrupt DB");
      }
      return $db;
    }
    die("");
  }

  // CHANGE THIS, use openssl to create something random
  $key = "HFGwe5IOfyfUg4tneOmSHJdni2lw27ReyJlFqwX4c";

  function encrypt($strng){
    global $key;
    $ivlen = openssl_cipher_iv_length($cipher="AES-128-CBC");
    $iv = openssl_random_pseudo_bytes($ivlen);
    $ciphertext_raw = openssl_encrypt($strng, $cipher, $key, $options=OPENSSL_RAW_DATA, $iv);
    $hmac = hash_hmac('sha256', $ciphertext_raw, $key, $as_binary=true);
    $ciphertext = base64_encode( $iv.$hmac.$ciphertext_raw );
    return $ciphertext;
  }

  function decrypt($ciphertext){
    $c = base64_decode($ciphertext);
    $ivlen = openssl_cipher_iv_length($cipher="AES-128-CBC");
    $iv = substr($c, 0, $ivlen);
    $hmac = substr($c, $ivlen, $sha2len=32);
    $ciphertext_raw = substr($c, $ivlen+$sha2len);
    $original_plaintext = openssl_decrypt($ciphertext_raw, $cipher, $key, $options=OPENSSL_RAW_DATA, $iv);
    $calcmac = hash_hmac('sha256', $ciphertext_raw, $key, $as_binary=true);
    if (hash_equals($hmac, $calcmac)){
      return $original_plaintext;
    }
    return null;
  }

  function b64urlsafe($strng){
    $strng = str_replace("/", "-sl-", $strng);
    $strng = str_replace("+", "-pl-", $strng);
    $strng = str_replace("=", "-eq-", $strng);
    return $strng;
  }
  function b64revert($strng){
    $strng = str_replace("-sl-", "/", $strng);
    $strng = str_replace("-pl-", "+", $strng);
    $strng = str_replace("-eq-", "=", $strng);
    return $strng;
  }
  function endsWith($haystack, $needle){
    $length = strlen($needle);
    return $length === 0 || (substr($haystack, -$length) === $needle);
  }
?>
