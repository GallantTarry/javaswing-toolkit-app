# -*- coding: utf-8 -*-
"""
[006] Excel数据批量生成Word报告
场景：读取Excel每一行数据，填入Word模板的 {{占位符}} 中，批量生成排版精美的Word。
核心库：docxtpl / pandas / tkinter
"""
import os
import re
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
import pandas as pd

try:
    from docxtpl import DocxTemplate
except ImportError:
    DocxTemplate = None

def clean_filename(filename):
    """清洗文件名，防止包含Windows系统不允许的特殊字符"""
    if pd.isna(filename) or str(filename).strip() == "":
        return "未命名报告"
    safe_name = re.sub(r'[\\/:*?"<>|]', '_', str(filename))
    return safe_name.strip()

def select_word_tpl():
    """选择排版好的 Word 模板"""
    file_path = filedialog.askopenfilename(
        title="请选择做好的 Word 模板文件",
        filetypes=[("Word 模板", "*.docx")]
    )
    if file_path:
        entry_tpl.config(state=tk.NORMAL)
        entry_tpl.delete(0, tk.END)
        entry_tpl.insert(0, file_path)
        entry_tpl.config(state=tk.DISABLED)

def select_excel_data():
    """选择 Excel 数据源并极速解析表头"""
    file_path = filedialog.askopenfilename(
        title="请选择包含数据的 Excel 文件",
        filetypes=[("Excel 数据表", "*.xlsx;*.xls")]
    )
    if file_path:
        entry_data.config(state=tk.NORMAL)
        entry_data.delete(0, tk.END)
        entry_data.insert(0, file_path)
        entry_data.config(state=tk.DISABLED)
        
        try:
            # 瞬间读取表头，供用户选择用哪一列来给新文件命名
            df_headers = pd.read_excel(file_path, nrows=0)
            columns = list(df_headers.columns.astype(str))
            
            combo_name['values'] = columns
            if columns:
                combo_name.current(0)
            btn_start.config(state=tk.NORMAL)
        except Exception as e:
            messagebox.showerror("读取失败", f"无法解析 Excel 表头，文件可能被占用！\n报错: {e}")
            combo_name['values'] = []
            combo_name.set('')
            btn_start.config(state=tk.DISABLED)

def start_generation():
    """执行批量生成逻辑"""
    tpl_path = entry_tpl.get()
    data_path = entry_data.get()
    name_col = combo_name.get()
    
    if not tpl_path or not data_path or not name_col:
        messagebox.showwarning("操作提示", "请确保【模板】、【数据源】和【命名列】都已选择完毕！")
        return

    if DocxTemplate is None:
        messagebox.showerror("缺少核心库", "检测到未安装 docxtpl。\n请在终端运行: pip install docxtpl pandas openpyxl")
        return

    # 锁定按钮
    btn_start.config(text="疯狂生成报告中...", state=tk.DISABLED)
    root.update()

    try:
        # 读取全量数据，把空单元格替换为空字符串，防止在Word里打出 'nan'
        df = pd.read_excel(data_path)
        df = df.fillna("")
        
        # 建立专属的输出文件夹
        base_dir = os.path.dirname(data_path)
        base_name = os.path.splitext(os.path.basename(data_path))[0]
        output_dir = os.path.join(base_dir, f"{base_name}_批量Word报告输出")
        
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        success_count = 0
        error_count = 0
        
        for index, row in df.iterrows():
            try:
                # 把这一行的数据转为 {列名: 值} 的字典
                context = row.to_dict()
                
                # 加载模板并注入数据
                doc = DocxTemplate(tpl_path)
                doc.render(context)
                
                # 按照用户选定的列给文件命名
                file_name_val = str(row[name_col])
                safe_name = clean_filename(file_name_val)
                output_file = os.path.join(output_dir, f"{safe_name}.docx")
                
                doc.save(output_file)
                success_count += 1
            except Exception:
                error_count += 1

        # 完工播报
        msg = f"✅ 批量出具报告完成！\n\n共成功生成 {success_count} 份独立 Word 文档。\n全部保存在原数据表旁边的专属文件夹中。"
        if error_count > 0:
            msg += f"\n\n⚠️ 另有 {error_count} 份生成失败，请检查数据。"
            
        messagebox.showinfo("生成成功", msg)

    except Exception as e:
        messagebox.showerror("运行异常", f"生成过程中发生崩溃！\n报错详情: {e}")
    finally:
        btn_start.config(text="开始批量生成", state=tk.NORMAL)

# ================= 构建可视化操作面板 =================
root = tk.Tk()
root.title("[008] Excel 数据批量生成 Word 报告")
root.geometry("540x260")
root.resizable(False, False)
root.attributes('-topmost', True)

padx, pady = 15, 12

# 1. 模板选择
tk.Label(root, text="第一步: 选择 Word 模板:").grid(row=0, column=0, padx=padx, pady=pady, sticky="e")
entry_tpl = tk.Entry(root, width=38, state=tk.DISABLED)
entry_tpl.grid(row=0, column=1, pady=pady)
tk.Button(root, text="浏览...", command=select_word_tpl).grid(row=0, column=2, padx=10, pady=pady)

# 2. 数据源选择
tk.Label(root, text="第二步: 选择 Excel 数据:").grid(row=1, column=0, padx=padx, pady=pady, sticky="e")
entry_data = tk.Entry(root, width=38, state=tk.DISABLED)
entry_data.grid(row=1, column=1, pady=pady)
tk.Button(root, text="浏览...", command=select_excel_data).grid(row=1, column=2, padx=10, pady=pady)

# 3. 命名规则设定
tk.Label(root, text="第三步: 生成文件命名:").grid(row=2, column=0, padx=padx, pady=pady, sticky="e")
frame_naming = tk.Frame(root)
frame_naming.grid(row=2, column=1, sticky="w", pady=pady)
combo_name = ttk.Combobox(frame_naming, width=20, state="readonly")
combo_name.pack(side=tk.LEFT)
tk.Label(frame_naming, text=" (选择一列作为文件名)", fg="gray").pack(side=tk.LEFT, padx=5)

# 4. 执行按钮
btn_start = tk.Button(root, text="开始批量生成", bg="#3F51B5", fg="white", font=("Arial", 11, "bold"), width=20, command=start_generation, state=tk.DISABLED)
btn_start.grid(row=3, column=0, columnspan=3, pady=15)

if __name__ == "__main__":
    root.mainloop()