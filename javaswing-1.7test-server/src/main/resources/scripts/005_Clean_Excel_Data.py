# -*- coding: utf-8 -*-
import os
import re
import pandas as pd
import tkinter as tk
from tkinter import ttk, filedialog, messagebox

def clean_filename(filename):
    """清洗文件名，替换Windows不允许出现的非法字符"""
    if pd.isna(filename) or str(filename).strip() == "":
        return "空值或未指定"
    safe_name = re.sub(r'[\\/:*?"<>|]', '_', str(filename))
    return safe_name.strip()

def select_file():
    """选择总表文件并自动解析表头"""
    file_path = filedialog.askopenfilename(
        title="请选择需要拆分的总表 Excel 文件",
        filetypes=[("Excel 电子表格", "*.xlsx;*.xls")]
    )
    if file_path:
        # 1. 更新文件路径显示
        entry_file.config(state=tk.NORMAL)
        entry_file.delete(0, tk.END)
        entry_file.insert(0, file_path)
        entry_file.config(state=tk.DISABLED) # 设为只读防误删
        
        # 2. 秒读表头，智能填充下拉框
        try:
            # nrows=0 代表只读表头不读数据，不管表有几十万行，瞬间完成
            df_headers = pd.read_excel(file_path, nrows=0)
            columns = list(df_headers.columns.astype(str))
            
            combo_cols['values'] = columns
            if columns:
                combo_cols.current(0) # 默认选中第一列
            
            btn_start.config(state=tk.NORMAL)
        except Exception as e:
            messagebox.showerror("读取失败", f"无法解析该表格的列名，请检查文件是否被占用！\n\n报错: {e}")
            combo_cols['values'] = []
            combo_cols.set('')
            btn_start.config(state=tk.DISABLED)

def start_split():
    """核心分组拆分逻辑"""
    file_path = entry_file.get()
    split_col = combo_cols.get()
    
    if not file_path or not split_col:
        messagebox.showwarning("操作提示", "请先选择要拆分的文件，并在下拉框中指定列名！")
        return

    # 锁定按钮防误触
    btn_start.config(text="正在拼命拆分中...", state=tk.DISABLED)
    root.update()

    try:
        # 正式读取全量数据
        df = pd.read_excel(file_path)
        
        # 二次校验列名是否存在
        if split_col not in df.columns:
            messagebox.showerror("列名错误", f"表格中找不到列名 '{split_col}'，请重新选择！")
            reset_button()
            return

        # 创建专属输出文件夹
        base_dir = os.path.dirname(file_path)
        base_name = os.path.splitext(os.path.basename(file_path))[0]
        output_dir = os.path.join(base_dir, f"{base_name}_按【{split_col}】拆分结果")
        
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        # 执行核心拆分算法
        grouped = df.groupby(split_col, dropna=False)
        success_count = 0
        error_count = 0
        
        for name, group in grouped:
            safe_name = clean_filename(name)
            output_file_path = os.path.join(output_dir, f"{safe_name}.xlsx")
            try:
                group.to_excel(output_file_path, index=False)
                success_count += 1
            except Exception:
                error_count += 1

        # 构建完工播报
        msg = f"✅ 拆分顺利完成！\n\n共按【{split_col}】拆分出 {success_count} 个独立 Excel 文件。\n全部保存在原表所在目录的专属文件夹中。"
        if error_count > 0:
            msg += f"\n\n⚠️ 另有 {error_count} 个分组因特殊符号或未知原因保存失败。"
        
        messagebox.showinfo("拆分成功", msg)

    except Exception as e:
        messagebox.showerror("运行异常", f"拆分过程中发生崩溃，请检查原表数据！\n\n报错详情: {e}")
    finally:
        reset_button()

def reset_button():
    """恢复按钮状态"""
    btn_start.config(text="开始一键拆分", state=tk.NORMAL)


# ================= 构建可视化操作面板 =================
root = tk.Tk()
root.title("Excel 总表条件拆分工具")
root.geometry("520x200")
root.resizable(False, False)

# 强制窗口置顶，防遮挡假死
root.attributes('-topmost', True)

padx, pady = 15, 12

# 第一行：总表选择
tk.Label(root, text="待拆分的总表:").grid(row=0, column=0, padx=padx, pady=pady, sticky="e")
entry_file = tk.Entry(root, width=38, state=tk.DISABLED)
entry_file.grid(row=0, column=1, pady=pady)
tk.Button(root, text="浏览...", command=select_file).grid(row=0, column=2, padx=10, pady=pady)

# 第二行：拆分条件下拉框
tk.Label(root, text="拆分依据列:").grid(row=1, column=0, padx=padx, pady=pady, sticky="e")
combo_cols = ttk.Combobox(root, width=35, state="readonly")
combo_cols.grid(row=1, column=1, pady=pady, sticky="w")

# 第三行：操作按钮
btn_start = tk.Button(root, text="开始一键拆分", bg="#FF9800", fg="black", font=("Arial", 11, "bold"), width=18, command=start_split, state=tk.DISABLED)
btn_start.grid(row=2, column=0, columnspan=3, pady=15)

if __name__ == "__main__":
    root.mainloop()